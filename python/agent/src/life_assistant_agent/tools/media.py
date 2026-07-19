"""HTTP helpers for shared media (一起看过的)."""

from typing import Any

from ..client import JavaClient


async def media_list(
    client: JavaClient,
    media_type: str | None = None,
    status: str | None = None,
    page: int | None = None,
    size: int | None = None,
) -> Any:
    params: dict[str, str] = {}
    if media_type is not None:
        params["mediaType"] = media_type
    if status is not None:
        params["status"] = status
    if page is not None:
        params["page"] = str(page)
    if size is not None:
        params["size"] = str(size)
    return await client.get("/shared-media", params)


async def media_get(client: JavaClient, id: str) -> Any:
    return await client.get(f"/shared-media/{id}")


async def media_create(
    client: JavaClient,
    title: str,
    media_type: str,
    description: str | None = None,
    last_watched_at: str | None = None,
) -> Any:
    return await client.post_form(
        "/shared-media",
        {
            "title": title,
            "mediaType": media_type,
            "description": description,
            "lastWatchedAt": last_watched_at,
        },
    )


async def media_update(
    client: JavaClient,
    id: str,
    title: str | None = None,
    media_type: str | None = None,
    description: str | None = None,
    last_watched_at: str | None = None,
    is_finished: bool | None = None,
) -> Any:
    return await client.patch_form(
        f"/shared-media/{id}",
        {
            "title": title,
            "mediaType": media_type,
            "description": description,
            "lastWatchedAt": last_watched_at,
            "isFinished": is_finished,
        },
    )


from ..dispatch_util import invalid_action, require

_MEDIA_ACTIONS = ["list", "get", "create", "update"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _MEDIA_ACTIONS:
        return invalid_action(_MEDIA_ACTIONS)
    if action == "list":
        return await media_list(
            client,
            fields.get("media_type"),
            fields.get("status"),
            fields.get("page"),
            fields.get("size"),
        )
    if action == "get":
        err = require(fields, "id")
        return err or await media_get(client, fields["id"])
    if action == "create":
        err = require(fields, "title", "media_type")
        return err or await media_create(
            client,
            fields["title"],
            fields["media_type"],
            fields.get("description"),
            fields.get("last_watched_at"),
        )
    if action == "update":
        err = require(fields, "id")
        return err or await media_update(
            client,
            fields["id"],
            fields.get("title"),
            fields.get("media_type"),
            fields.get("description"),
            fields.get("last_watched_at"),
            fields.get("is_finished"),
        )
    return invalid_action(_MEDIA_ACTIONS)
