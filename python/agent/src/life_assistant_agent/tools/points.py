"""MCP tools for partner points operations."""

from typing import Any

from ..client import JavaClient


async def points_get(client: JavaClient) -> Any:
    return await client.get("/partner/points")


async def points_history(client: JavaClient, page: int | None = None,
                         size: int | None = None) -> Any:
    params: dict[str, str] = {}
    if page is not None:
        params["page"] = str(page)
    if size is not None:
        params["size"] = str(size)
    return await client.get("/partner/points/history", params)


async def points_change(client: JavaClient, points_change: int, reason: str) -> Any:
    return await client.post("/partner/points", {
        "pointsChange": points_change,
        "reason": reason,
    })
