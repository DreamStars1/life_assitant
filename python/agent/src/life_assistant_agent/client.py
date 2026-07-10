"""HTTP client for Java backend REST API."""

import os
from typing import Any

import httpx

JAVA_BASE_URL = os.environ.get("JAVA_BASE_URL", "http://localhost:8000")


class JavaClient:
    """HTTP client that forwards Bearer token to Java backend."""

    def __init__(self, token: str) -> None:
        self._client = httpx.AsyncClient(
            base_url=JAVA_BASE_URL,
            headers={"Authorization": f"Bearer {token}"},
            timeout=30.0,
        )

    async def close(self) -> None:
        await self._client.aclose()

    async def get(self, path: str, params: dict[str, Any] | None = None) -> Any:
        resp = await self._client.get(path, params=params)
        resp.raise_for_status()
        return self._unwrap(resp.json())

    async def post(self, path: str, body: dict[str, Any] | None = None) -> Any:
        resp = await self._client.post(path, json=body or {})
        resp.raise_for_status()
        return self._unwrap(resp.json())

    async def put(self, path: str, body: dict[str, Any] | None = None) -> Any:
        resp = await self._client.put(path, json=body or {})
        resp.raise_for_status()
        return self._unwrap(resp.json())

    async def patch(self, path: str, body: dict[str, Any] | None = None) -> Any:
        resp = await self._client.request("PATCH", path, json=body or {})
        resp.raise_for_status()
        return self._unwrap(resp.json())

    @staticmethod
    def _unwrap(data: dict[str, Any]) -> Any:
        """Unwrap standard ApiResponse {code, message, data} wrapper."""
        if "data" in data:
            return data["data"]
        return data
