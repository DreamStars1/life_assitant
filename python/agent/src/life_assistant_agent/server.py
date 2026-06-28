"""MCP Streamable HTTP Server for Life Assistant.

Extracts Bearer token from incoming headers and proxies to Java backend.
"""

import json
import logging
import os
from contextvars import ContextVar
from typing import Any

import uvicorn
from mcp.server.fastmcp import FastMCP
from starlette.types import ASGIApp, Receive, Scope, Send

from .client import JavaClient
from .tools import record as record_tools
from .tools import todo as todo_tools

logger = logging.getLogger(__name__)

HOST = os.environ.get("AGENT_HOST", "0.0.0.0")
PORT = int(os.environ.get("AGENT_PORT", "8089"))

# Context var to pass auth token from middleware to tool handlers
_auth_token: ContextVar[str | None] = ContextVar("auth_token", default=None)

mcp = FastMCP("life-assistant")


def _get_client() -> JavaClient:
    token = _auth_token.get()
    if not token:
        raise RuntimeError("Missing Authorization header")
    return JavaClient(token)


# ── Tools ──────────────────────────────────────────────────────────────

@mcp.tool(description="创建待办事项")
async def todo_create(
    title: str, description: str | None = None,
    priority: str | None = None, due_date: str | None = None,
    assign_to_partner: bool | None = None,
) -> str:
    """创建待办事项。

    Args:
        title: 标题
        description: 详细描述（可选）
        priority: 优先级 low / medium / high / urgent（可选，默认 medium）
        due_date: 截止日期，格式如 2026-06-29 或 2026-06-29T12:00:00（可选）
        assign_to_partner: 是否自动指派给伴侣（可选）
    """
    try:
        result = await todo_tools.todo_create(
            client, title, description, priority, due_date, assign_to_partner,
        )
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="查询待办列表，支持筛选")
async def todo_list(
    is_completed: bool | None = None, priority: str | None = None,
    start_due_date: str | None = None, end_due_date: str | None = None,
) -> str:
    """查询待办列表，支持筛选。

    Args:
        is_completed: 是否已完成（可选）
        priority: 优先级筛选 low / medium / high / urgent（可选）
        start_due_date: 截止日期范围起始，格式如 2026-06-29 或 2026-06-29T12:00:00（可选）
        end_due_date: 截止日期范围结束，格式如 2026-06-29 或 2026-06-29T12:00:00（可选）
    """
    try:
        result = await todo_tools.todo_list(client, is_completed, priority, start_due_date, end_due_date)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="获取首页最近的未完成待办")
async def todo_upcoming() -> str:
    client = _get_client()
    try:
        result = await todo_tools.todo_upcoming(client)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="获取待办详情")
async def todo_get(id: str) -> str:
    client = _get_client()
    try:
        result = await todo_tools.todo_get(client, id)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="更新待办事项")
async def todo_update(
    id: str, title: str | None = None, description: str | None = None,
    priority: str | None = None, due_date: str | None = None,
) -> str:
    """更新待办事项。

    Args:
        id: 待办 ID
        title: 新标题（可选）
        description: 新详细描述（可选）
        priority: 新优先级 low / medium / high / urgent（可选）
        due_date: 新截止日期，格式如 2026-06-29 或 2026-06-29T12:00:00（可选）
    """
    try:
        result = await todo_tools.todo_update(client, id, title, description, priority, due_date)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="切换待办完成状态")
async def todo_toggle(id: str) -> str:
    client = _get_client()
    try:
        result = await todo_tools.todo_toggle(client, id)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="确认收到待办（仅被指派者可操作）")
async def todo_acknowledge(id: str, message: str | None = None) -> str:
    client = _get_client()
    try:
        result = await todo_tools.todo_acknowledge(client, id, message)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="记录一起做过的事")
async def record_create(title: str, content: str | None = None, occurred_at: str | None = None) -> str:
    client = _get_client()
    try:
        result = await record_tools.record_create(client, title, content, occurred_at)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="查询共享记录列表，支持时间范围筛选")
async def record_list(start: str | None = None, end: str | None = None) -> str:
    client = _get_client()
    try:
        result = await record_tools.record_list(client, start, end)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="获取共享记录详情")
async def record_get(id: str) -> str:
    client = _get_client()
    try:
        result = await record_tools.record_get(client, id)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="更新共享记录")
async def record_update(
    id: str, title: str | None = None, content: str | None = None,
    occurred_at: str | None = None,
) -> str:
    client = _get_client()
    try:
        result = await record_tools.record_update(client, id, title, content, occurred_at)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


# ── ASGI Auth wrapper (preserves lifespan) ────────────────────────────

class AuthASGIWrapper:
    """Wraps the MCP ASGI app to extract Bearer token without breaking lifespan."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] == "http":
            path = scope.get("path", "")
            if path == "/health":
                await self._health_response(send)
                return
            headers = dict(scope.get("headers", []))
            auth = headers.get(b"authorization", b"").decode()
            if auth.startswith("Bearer "):
                _auth_token.set(auth.removeprefix("Bearer "))
        await self.app(scope, receive, send)

    @staticmethod
    async def _health_response(send: Send) -> None:
        await send({
            "type": "http.response.start",
            "status": 200,
            "headers": [(b"content-type", b"application/json")],
        })
        await send({
            "type": "http.response.body",
            "body": b'{"status":"ok"}',
        })


# ── Entry point ────────────────────────────────────────────────────────

def main() -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    )
    logger.info("Starting Life Assistant MCP Agent on %s:%s", HOST, PORT)

    starlette_app = mcp.streamable_http_app()
    wrapped = AuthASGIWrapper(starlette_app)

    config = uvicorn.Config(wrapped, host=HOST, port=PORT, log_level="info")
    server = uvicorn.Server(config)
    server.run()
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())
