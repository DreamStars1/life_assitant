"""HTTP helpers for schedule events."""

from typing import Any

from ..client import JavaClient


def _ensure_full_datetime(val: str | None) -> str | None:
    if val is None:
        return None
    if "T" not in val:
        return val + "T00:00:00"
    return val


async def schedule_list(
    client: JavaClient, from_: str, to: str, scope: str = "me",
) -> Any:
    params = {
        "from": _ensure_full_datetime(from_),
        "to": _ensure_full_datetime(to),
    }
    path = "/schedule/events/partner" if scope == "partner" else "/schedule/events"
    return await client.get(path, params)


async def schedule_create(
    client: JavaClient,
    title: str,
    start_at: str,
    end_at: str,
    note: str | None = None,
    recurrence: str | None = None,
    recurrence_end_date: str | None = None,
) -> Any:
    body: dict[str, Any] = {
        "title": title,
        "startAt": _ensure_full_datetime(start_at),
        "endAt": _ensure_full_datetime(end_at),
    }
    if note is not None:
        body["note"] = note
    if recurrence is not None:
        body["recurrence"] = recurrence
    if recurrence_end_date is not None:
        body["recurrenceEndDate"] = recurrence_end_date
    return await client.post("/schedule/events", body)


async def schedule_update(
    client: JavaClient,
    id: str,
    title: str | None = None,
    start_at: str | None = None,
    end_at: str | None = None,
    note: str | None = None,
    recurrence: str | None = None,
    recurrence_end_date: str | None = None,
) -> Any:
    body: dict[str, Any] = {}
    if title is not None:
        body["title"] = title
    v = _ensure_full_datetime(start_at)
    if v is not None:
        body["startAt"] = v
    v = _ensure_full_datetime(end_at)
    if v is not None:
        body["endAt"] = v
    if note is not None:
        body["note"] = note
    if recurrence is not None:
        body["recurrence"] = recurrence
    if recurrence_end_date is not None:
        body["recurrenceEndDate"] = recurrence_end_date
    return await client.patch(f"/schedule/events/{id}", body)
