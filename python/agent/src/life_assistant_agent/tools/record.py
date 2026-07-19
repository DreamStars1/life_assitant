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


from ..dispatch_util import invalid_action, require

_RECORD_ACTIONS = ["list", "get", "create", "update"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _RECORD_ACTIONS:
        return invalid_action(_RECORD_ACTIONS)
    if action == "list":
        return await record_list(client, fields.get("start"), fields.get("end"))
    if action == "get":
        err = require(fields, "id")
        return err or await record_get(client, fields["id"])
    if action == "create":
        err = require(fields, "title")
        return err or await record_create(
            client,
            fields["title"],
            fields.get("content"),
            fields.get("occurred_at"),
        )
    if action == "update":
        err = require(fields, "id")
        return err or await record_update(
            client,
            fields["id"],
            fields.get("title"),
            fields.get("content"),
            fields.get("occurred_at"),
        )
    return invalid_action(_RECORD_ACTIONS)
