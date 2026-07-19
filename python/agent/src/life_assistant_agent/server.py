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
from mcp.server.transport_security import TransportSecuritySettings
from starlette.types import ASGIApp, Receive, Scope, Send

from .client import JavaClient
from .tools import checkin as checkin_tools
from .tools import media as media_tools
from .tools import points as points_tools
from .tools import record as record_tools
from .tools import schedule as schedule_tools
from .tools import todo as todo_tools

logger = logging.getLogger(__name__)

HOST = os.environ.get("AGENT_HOST", "0.0.0.0")
PORT = int(os.environ.get("AGENT_PORT", "8089"))

# Context var to pass auth token from middleware to tool handlers
_auth_token: ContextVar[str | None] = ContextVar("auth_token", default=None)

mcp = FastMCP(
    "life-assistant",
    transport_security=TransportSecuritySettings(
        enable_dns_rebinding_protection=True,
        allowed_hosts=[
            "mcp.life-assitant.top",
            "mcp.life-assitant.top:*",
            "localhost",
            "localhost:*",
            "127.0.0.1",
            "127.0.0.1:*",
        ],
        allowed_origins=[
            "https://mcp.life-assitant.top",
            "https://mcp.life-assitant.top:*",
        ],
    ),
)


def _get_client() -> JavaClient:
    token = _auth_token.get()
    if not token:
        raise RuntimeError("Missing Authorization header")
    return JavaClient(token)


# ── Tools ──────────────────────────────────────────────────────────────

async def _run(domain_dispatch, action: str, **fields) -> str:
    client = _get_client()
    try:
        result = await domain_dispatch(client, action, **fields)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()


@mcp.tool(description="待办：action=list|upcoming|get|create|update|toggle|acknowledge")
async def todo(
    action: str,
    id: str | None = None,
    title: str | None = None,
    description: str | None = None,
    priority: str | None = None,
    due_date: str | None = None,
    assign_to_partner: bool | None = None,
    is_completed: bool | None = None,
    start_due_date: str | None = None,
    end_due_date: str | None = None,
    message: str | None = None,
) -> str:
    return await _run(
        todo_tools.dispatch, action,
        id=id, title=title, description=description, priority=priority,
        due_date=due_date, assign_to_partner=assign_to_partner,
        is_completed=is_completed, start_due_date=start_due_date,
        end_due_date=end_due_date, message=message,
    )


@mcp.tool(description="一起做过的事（不是看过的媒体）：action=list|get|create|update")
async def record(
    action: str,
    id: str | None = None,
    title: str | None = None,
    content: str | None = None,
    start: str | None = None,
    end: str | None = None,
    occurred_at: str | None = None,
) -> str:
    return await _run(
        record_tools.dispatch, action,
        id=id, title=title, content=content, start=start, end=end, occurred_at=occurred_at,
    )


@mcp.tool(description="日程：action=list|create|update；list 时 scope=me|partner")
async def schedule(
    action: str,
    from_: str | None = None,
    to: str | None = None,
    scope: str | None = None,
    id: str | None = None,
    title: str | None = None,
    start_at: str | None = None,
    end_at: str | None = None,
    note: str | None = None,
    recurrence: str | None = None,
    recurrence_end_date: str | None = None,
) -> str:
    """日程（独立时间块，非待办）。

    Args:
        action: list / create / update
        from_: list 区间起点，如 2026-07-01 或 2026-07-01T00:00:00
        to: list 区间终点
        scope: me（默认）或 partner
        id: update 时必填
        title, start_at, end_at: create 必填；update 可选
        note, recurrence, recurrence_end_date: 可选；recurrence=none|daily|weekly
    """
    return await _run(
        schedule_tools.dispatch, action,
        from_=from_, to=to, scope=scope, id=id, title=title,
        start_at=start_at, end_at=end_at, note=note,
        recurrence=recurrence, recurrence_end_date=recurrence_end_date,
    )


@mcp.tool(description="一起看过的内容（电影/书/剧，不是做过的事）：action=list|get|create|update；无封面上传")
async def media(
    action: str,
    id: str | None = None,
    title: str | None = None,
    media_type: str | None = None,
    description: str | None = None,
    last_watched_at: str | None = None,
    is_finished: bool | None = None,
    status: str | None = None,
    page: int | None = None,
    size: int | None = None,
) -> str:
    return await _run(
        media_tools.dispatch, action,
        id=id, title=title, media_type=media_type, description=description,
        last_watched_at=last_watched_at, is_finished=is_finished,
        status=status, page=page, size=size,
    )


@mcp.tool(description="伴侣积分：action=get|history|change")
async def points(
    action: str,
    page: int | None = None,
    size: int | None = None,
    points_change: int | None = None,
    reason: str | None = None,
) -> str:
    return await _run(
        points_tools.dispatch, action,
        page=page, size=size, points_change=points_change, reason=reason,
    )


@mcp.tool(description="作息打卡：action=do|today|weekly；do 时 checkin_type=wake|sleep")
async def checkin(
    action: str,
    checkin_type: str | None = None,
) -> str:
    return await _run(checkin_tools.dispatch, action, checkin_type=checkin_type)


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

    config = uvicorn.Config(
        wrapped, host=HOST, port=PORT, log_level="info",
        proxy_headers=True, forwarded_allow_ips="*",
    )
    server = uvicorn.Server(config)
    server.run()
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())
