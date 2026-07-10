"""MCP tools for shared record operations."""

from typing import Any

from ..client import JavaClient


def _ensure_full_datetime(val: str | None) -> str | None:
    """Java LocalDateTime needs full format 'yyyy-MM-ddTHH:mm:ss'. Append midnight if date-only."""
    if val is None:
        return None
    if "T" not in val:
        return val + "T00:00:00"
    return val


async def record_create(client: JavaClient, title: str, content: str | None = None,
                        occurred_at: str | None = None) -> Any:
    body: dict[str, Any] = {"title": title}
    if content is not None:
        body["content"] = content
    v = _ensure_full_datetime(occurred_at)
    if v is not None:
        body["occurredAt"] = v
    return await client.post("/shared-records", body)


async def record_list(client: JavaClient, start: str | None = None,
                      end: str | None = None) -> Any:
    params: dict[str, str] = {}
    v = _ensure_full_datetime(start)
    if v is not None:
        params["start"] = v
    v = _ensure_full_datetime(end)
    if v is not None:
        params["end"] = v
    return await client.get("/shared-records", params)


async def record_get(client: JavaClient, id: str) -> Any:
    return await client.get(f"/shared-records/{id}")


async def record_update(client: JavaClient, id: str, title: str | None = None,
                        content: str | None = None,
                        occurred_at: str | None = None) -> Any:
    body: dict[str, Any] = {}
    if title is not None:
        body["title"] = title
    if content is not None:
        body["content"] = content
    v = _ensure_full_datetime(occurred_at)
    if v is not None:
        body["occurredAt"] = v
    return await client.patch(f"/shared-records/{id}", body)
