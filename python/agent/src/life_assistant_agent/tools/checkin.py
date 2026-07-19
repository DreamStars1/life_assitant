"""MCP tools for partner sleep/wake checkin operations."""

from typing import Any

from ..client import JavaClient


async def checkin_do(client: JavaClient, checkin_type: str) -> Any:
    return await client.post("/partner/checkin", {"checkinType": checkin_type})


async def checkin_today(client: JavaClient) -> Any:
    return await client.get("/partner/checkin/today")


async def checkin_weekly(client: JavaClient) -> Any:
    return await client.get("/partner/checkin/weekly")


from ..dispatch_util import invalid_action, require

_CHECKIN_ACTIONS = ["do", "today", "weekly"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _CHECKIN_ACTIONS:
        return invalid_action(_CHECKIN_ACTIONS)
    if action == "do":
        err = require(fields, "checkin_type")
        return err or await checkin_do(client, fields["checkin_type"])
    if action == "today":
        return await checkin_today(client)
    if action == "weekly":
        return await checkin_weekly(client)
    return invalid_action(_CHECKIN_ACTIONS)
