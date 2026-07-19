"""Structured error helpers for domain-tool action dispatch."""

from __future__ import annotations

from typing import Any


def invalid_action(allowed: list[str]) -> dict[str, Any]:
    return {"error": "invalid_action", "allowed": sorted(allowed)}


def missing_field(field: str) -> dict[str, Any]:
    return {"error": "missing_field", "field": field}


def require(fields: dict[str, Any], *names: str) -> dict[str, Any] | None:
    for name in names:
        val = fields.get(name)
        if val is None or val == "":
            return missing_field(name)
    return None
