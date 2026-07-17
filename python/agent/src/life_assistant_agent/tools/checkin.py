"""MCP tools for partner sleep/wake checkin operations."""

from typing import Any

from ..client import JavaClient


async def checkin_do(client: JavaClient, checkin_type: str) -> Any:
    return await client.post("/partner/checkin", {"checkinType": checkin_type})


async def checkin_today(client: JavaClient) -> Any:
    return await client.get("/partner/checkin/today")


async def checkin_weekly(client: JavaClient) -> Any:
    return await client.get("/partner/checkin/weekly")
