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


from ..dispatch_util import invalid_action, require

_POINTS_ACTIONS = ["get", "history", "change"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _POINTS_ACTIONS:
        return invalid_action(_POINTS_ACTIONS)
    if action == "get":
        return await points_get(client)
    if action == "history":
        return await points_history(client, fields.get("page"), fields.get("size"))
    if action == "change":
        err = require(fields, "points_change", "reason")
        return err or await points_change(client, fields["points_change"], fields["reason"])
    return invalid_action(_POINTS_ACTIONS)
