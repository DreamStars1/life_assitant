"""MCP tools for todo operations."""

from typing import Any

from ..client import JavaClient


def _ensure_full_datetime(val: str | None) -> str | None:
    """Java LocalDateTime needs full format 'yyyy-MM-ddTHH:mm:ss'. Append midnight if date-only."""
    if val is None:
        return None
    if "T" not in val:
        return val + "T00:00:00"
    return val


async def todo_create(client: JavaClient, title: str, description: str | None = None,
                      priority: str | None = "medium", due_date: str | None = None,
                      assign_to_partner: bool | None = None) -> Any:
    body: dict[str, Any] = {"title": title, "priority": priority or "medium"}
    if description is not None:
        body["description"] = description
    v = _ensure_full_datetime(due_date)
    if v is not None:
        body["dueDate"] = v
    if assign_to_partner:
        user = await client.get("/users/me")
        partner_id = user.get("partnerId")
        if partner_id:
            body["assignedTo"] = partner_id
    return await client.post("/todos", body)


async def todo_list(client: JavaClient, is_completed: bool | None = None,
                    priority: str | None = None,
                    start_due_date: str | None = None,
                    end_due_date: str | None = None) -> Any:
    params: dict[str, str] = {}
    if is_completed is not None:
        params["isCompleted"] = str(is_completed).lower()
    if priority is not None:
        params["priority"] = priority
    val = _ensure_full_datetime(start_due_date)
    if val is not None:
        params["startDueDate"] = val
    val = _ensure_full_datetime(end_due_date)
    if val is not None:
        params["endDueDate"] = val
    return await client.get("/todos", params)


async def todo_upcoming(client: JavaClient) -> Any:
    return await client.get("/todos/upcoming")


async def todo_get(client: JavaClient, id: str) -> Any:
    return await client.get(f"/todos/{id}")


async def todo_update(client: JavaClient, id: str, title: str | None = None,
                      description: str | None = None, priority: str | None = None,
                      due_date: str | None = None) -> Any:
    body: dict[str, Any] = {}
    if title is not None:
        body["title"] = title
    if description is not None:
        body["description"] = description
    if priority is not None:
        body["priority"] = priority
    v = _ensure_full_datetime(due_date)
    if v is not None:
        body["dueDate"] = v
    return await client.patch(f"/todos/{id}", body)


async def todo_toggle(client: JavaClient, id: str) -> Any:
    return await client.post(f"/todos/{id}/toggle")


async def todo_acknowledge(client: JavaClient, id: str, message: str | None = None) -> Any:
    body = {}
    if message is not None:
        body["message"] = message
    return await client.post(f"/todos/{id}/acknowledge", body)
