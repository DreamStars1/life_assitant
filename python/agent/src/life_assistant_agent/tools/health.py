"""MCP tools for health butler operations."""

from typing import Any

from ..client import JavaClient
from ..dispatch_util import invalid_action, require

_HEALTH_ACTIONS = [
    "profile_get",
    "profile_update",
    "meal_list",
    "meal_upsert",
    "meal_delete",
    "weight_list",
    "weight_add",
    "daily_get",
    "daily_update",
    "memory_list",
    "memory_add",
    "tolerance_list",
    "tolerance_add",
    "trigger_list",
    "trigger_add",
    "summary",
]


def _profile_body(fields: dict[str, Any]) -> dict[str, Any]:
    body: dict[str, Any] = {}
    for src, dst in (
        ("display_name", "displayName"),
        ("motto", "motto"),
        ("height_cm", "heightCm"),
        ("target_kg", "targetKg"),
        ("resting_kcal", "restingKcal"),
    ):
        if fields.get(src) is not None:
            body[dst] = fields[src]
    return body


def _daily_body(fields: dict[str, Any]) -> dict[str, Any]:
    body: dict[str, Any] = {"date": fields["date"]}
    for src, dst in (
        ("stomach_status", "stomachStatus"),
        ("stomach_note", "stomachNote"),
        ("cycle_phase", "cyclePhase"),
        ("cycle_day", "cycleDay"),
    ):
        if fields.get(src) is not None:
            body[dst] = fields[src]
    if "burn_kcal" in fields:
        body["burnKcal"] = fields["burn_kcal"]
    return body


def _page_params(fields: dict[str, Any]) -> dict[str, str]:
    params: dict[str, str] = {}
    if fields.get("page") is not None:
        params["page"] = str(fields["page"])
    if fields.get("page_size") is not None:
        params["pageSize"] = str(fields["page_size"])
    return params


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _HEALTH_ACTIONS:
        return invalid_action(_HEALTH_ACTIONS)

    if action == "profile_get":
        return await client.get("/health/profile")

    if action == "profile_update":
        return await client.put("/health/profile", _profile_body(fields))

    if action == "meal_list":
        err = require(fields, "date")
        return err or await client.get("/health/meals", {"date": fields["date"]})

    if action == "meal_upsert":
        err = require(fields, "date", "meal_type", "food")
        if err:
            return err
        body: dict[str, Any] = {
            "date": fields["date"],
            "mealType": fields["meal_type"],
            "food": fields["food"],
        }
        if fields.get("protein_g") is not None:
            body["proteinG"] = fields["protein_g"]
        if fields.get("kcal") is not None:
            body["kcal"] = fields["kcal"]
        if fields.get("feedback") is not None:
            body["feedback"] = fields["feedback"]
        return await client.put("/health/meals", body)

    if action == "meal_delete":
        err = require(fields, "id")
        return err or await client.delete(f"/health/meals/{fields['id']}")

    if action == "weight_list":
        params: dict[str, str] = {}
        if fields.get("from_") is not None:
            params["from"] = fields["from_"]
        if fields.get("to") is not None:
            params["to"] = fields["to"]
        if fields.get("type_") is not None:
            params["type"] = fields["type_"]
        return await client.get("/health/weights", params or None)

    if action == "weight_add":
        err = require(fields, "date", "weight_type", "kg")
        if err:
            return err
        body = {
            "date": fields["date"],
            "weightType": fields["weight_type"],
            "kg": fields["kg"],
        }
        if fields.get("standard") is not None:
            body["standard"] = fields["standard"]
        return await client.post("/health/weights", body)

    if action == "daily_get":
        err = require(fields, "date")
        return err or await client.get("/health/daily", {"date": fields["date"]})

    if action == "daily_update":
        err = require(fields, "date")
        return err or await client.put("/health/daily", _daily_body(fields))

    if action == "memory_list":
        return await client.get("/health/memories", _page_params(fields) or None)

    if action == "memory_add":
        err = require(fields, "title")
        if err:
            return err
        body: dict[str, Any] = {"title": fields["title"]}
        if fields.get("detail") is not None:
            body["detail"] = fields["detail"]
        return await client.post("/health/memories", body)

    if action == "tolerance_list":
        return await client.get("/health/tolerances", _page_params(fields) or None)

    if action == "tolerance_add":
        err = require(fields, "name", "level")
        if err:
            return err
        return await client.post("/health/tolerances", {
            "name": fields["name"],
            "level": fields["level"],
        })

    if action == "trigger_list":
        return await client.get("/health/triggers", _page_params(fields) or None)

    if action == "trigger_add":
        err = require(fields, "name", "stars")
        if err:
            return err
        body = {"name": fields["name"], "stars": fields["stars"]}
        if fields.get("note") is not None:
            body["note"] = fields["note"]
        return await client.post("/health/triggers", body)

    if action == "summary":
        return await client.get("/health/summary")

    return invalid_action(_HEALTH_ACTIONS)
