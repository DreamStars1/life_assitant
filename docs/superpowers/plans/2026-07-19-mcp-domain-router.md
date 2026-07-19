# MCP Domain Router Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Python MCP Agent 的 17 个细粒度工具收成 6 个域路由工具，并新增日程（`schedule`）与一起看过的（`media`）覆盖。

**Architecture:** 每个域一个 `@mcp.tool`，必填 `action` 分发到 `tools/{domain}.py`；校验失败返回结构化错误 JSON；`media` create/update 经 `JavaClient` form 方法调用现网 multipart REST（不传封面）。鉴权与部署不变。

**Tech Stack:** Python 3.11+ / FastMCP / httpx / uvicorn；测试用标准库 `unittest` + `unittest.mock`（不新增 pytest）。

**Spec:** `docs/superpowers/specs/2026-07-19-mcp-domain-router-design.md`

## Global Constraints

- 对外工具数必须为 **6**：`todo` / `record` / `schedule` / `media` / `points` / `checkin`
- Breaking：删除全部旧名（`todo_list` 等），不做别名兼容
- Agent 精简面：不做 delete、日程邀约、media 评论/进度/封面
- `schedule.list` 支持 `scope=me|partner`；`media` 无 progress
- `record` ≠ `media`：description 必须写清「做过的事」vs「看过的内容」
- 错误：`{"error":"invalid_action","allowed":[...]}` / `{"error":"missing_field","field":"..."}`
- PowerShell：`git commit -m "msg"`，路径用双引号
- 不改 Java REST 形状；不引入新 Python 依赖

---

## 文件结构

```
python/agent/
  src/life_assistant_agent/
    client.py                 — 增加 post_form / patch_form
    dispatch_util.py          — invalid_action / missing_field 辅助
    server.py                 — 仅注册 6 个域工具
    tools/
      todo.py                 — 保留 HTTP 封装 + 新增 dispatch()
      record.py               — 同上
      points.py               — 同上
      checkin.py              — 同上
      schedule.py             — 新建：HTTP + dispatch
      media.py                — 新建：HTTP + dispatch
  tests/
    test_dispatch_util.py
    test_client_form.py
    test_schedule_media_http.py
    test_domain_dispatch.py
docs/superpowers/specs/
  2026-06-28-mcp-agent-module-design.md   — 工具列表改为 6 域（附注指向新 spec）
```

---

### Task 1: dispatch_util + 失败测试

**Files:**
- Create: `python/agent/src/life_assistant_agent/dispatch_util.py`
- Create: `python/agent/tests/test_dispatch_util.py`
- Create: `python/agent/tests/__init__.py`（空文件）

**Interfaces:**
- Produces:
  - `invalid_action(allowed: list[str]) -> dict`
  - `missing_field(field: str) -> dict`
  - `require(fields: dict, *names: str) -> dict | None`（缺字段返回 missing_field，否则 None）

- [ ] **Step 1: 写失败测试**

```python
# python/agent/tests/test_dispatch_util.py
import unittest

from life_assistant_agent.dispatch_util import invalid_action, missing_field, require


class DispatchUtilTest(unittest.TestCase):
    def test_invalid_action(self):
        self.assertEqual(
            invalid_action(["list", "get"]),
            {"error": "invalid_action", "allowed": ["get", "list"]},
        )

    def test_missing_field(self):
        self.assertEqual(missing_field("id"), {"error": "missing_field", "field": "id"})

    def test_require_ok(self):
        self.assertIsNone(require({"id": "1", "title": "a"}, "id", "title"))

    def test_require_missing(self):
        self.assertEqual(require({"id": "1"}, "id", "title"), missing_field("title"))

    def test_require_empty_string_is_missing(self):
        self.assertEqual(require({"id": ""}, "id"), missing_field("id"))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 跑测试确认失败**

Run（在 `python/agent` 目录）:

```powershell
python -m unittest tests.test_dispatch_util -v
```

Expected: FAIL（`ModuleNotFoundError: life_assistant_agent.dispatch_util` 或 import 失败）

- [ ] **Step 3: 实现 dispatch_util**

```python
# python/agent/src/life_assistant_agent/dispatch_util.py
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
```

并创建空的 `python/agent/tests/__init__.py`。

- [ ] **Step 4: 跑测试确认通过**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_dispatch_util -v
```

Expected: PASS（4 tests）

- [ ] **Step 5: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/dispatch_util.py" "python/agent/tests/__init__.py" "python/agent/tests/test_dispatch_util.py"
git commit -m "feat(agent): add MCP dispatch error helpers"
```

---

### Task 2: JavaClient form 方法

**Files:**
- Modify: `python/agent/src/life_assistant_agent/client.py`
- Create: `python/agent/tests/test_client_form.py`

**Interfaces:**
- Consumes: 现有 `JavaClient` / `_unwrap`
- Produces:
  - `async def post_form(self, path: str, data: dict[str, Any]) -> Any`
  - `async def patch_form(self, path: str, data: dict[str, Any]) -> Any`
  - 仅发送非 `None` 字段；`bool` 转为小写 `"true"`/`"false"` 字符串（Spring `@RequestParam`）

- [ ] **Step 1: 写失败测试**

```python
# python/agent/tests/test_client_form.py
import unittest
from unittest.mock import AsyncMock, MagicMock, patch

from life_assistant_agent.client import JavaClient


class ClientFormTest(unittest.IsolatedAsyncioTestCase):
    async def test_post_form_sends_multipart_fields(self):
        client = JavaClient("tok")
        mock_resp = MagicMock()
        mock_resp.json.return_value = {"data": {"id": "m1"}}
        mock_resp.raise_for_status = MagicMock()
        client._client.post = AsyncMock(return_value=mock_resp)
        try:
            result = await client.post_form(
                "/shared-media",
                {"title": "X", "mediaType": "movie", "description": None},
            )
            self.assertEqual(result, {"id": "m1"})
            kwargs = client._client.post.await_args.kwargs
            self.assertEqual(kwargs["data"], {"title": "X", "mediaType": "movie"})
        finally:
            await client.close()

    async def test_patch_form_bool_as_lowercase_str(self):
        client = JavaClient("tok")
        mock_resp = MagicMock()
        mock_resp.json.return_value = {"data": {"ok": True}}
        mock_resp.raise_for_status = MagicMock()
        client._client.request = AsyncMock(return_value=mock_resp)
        try:
            await client.patch_form("/shared-media/1", {"isFinished": True})
            kwargs = client._client.request.await_args.kwargs
            self.assertEqual(kwargs["data"]["isFinished"], "true")
        finally:
            await client.close()


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 跑测试确认失败**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_client_form -v
```

Expected: FAIL（`AttributeError: post_form`）

- [ ] **Step 3: 实现 form 方法**

在 `JavaClient` 类中增加：

```python
@staticmethod
def _form_data(data: dict[str, Any]) -> dict[str, str]:
    out: dict[str, str] = {}
    for k, v in data.items():
        if v is None:
            continue
        if isinstance(v, bool):
            out[k] = "true" if v else "false"
        else:
            out[k] = str(v)
    return out

async def post_form(self, path: str, data: dict[str, Any]) -> Any:
    resp = await self._client.post(path, data=self._form_data(data))
    resp.raise_for_status()
    return self._unwrap(resp.json())

async def patch_form(self, path: str, data: dict[str, Any]) -> Any:
    resp = await self._client.request("PATCH", path, data=self._form_data(data))
    resp.raise_for_status()
    return self._unwrap(resp.json())
```

（`from typing import Any` 已存在。）

- [ ] **Step 4: 跑测试确认通过**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_client_form -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/client.py" "python/agent/tests/test_client_form.py"
git commit -m "feat(agent): add JavaClient form helpers for shared-media"
```

---

### Task 3: schedule / media HTTP 封装

**Files:**
- Create: `python/agent/src/life_assistant_agent/tools/schedule.py`
- Create: `python/agent/src/life_assistant_agent/tools/media.py`
- Create: `python/agent/tests/test_schedule_media_http.py`

**Interfaces:**
- Consumes: `JavaClient.get/post/patch/post_form/patch_form`
- Produces（HTTP 层，无 dispatch）:
  - `schedule_list(client, from_, to, scope="me")`
  - `schedule_create(client, title, start_at, end_at, note=None, recurrence=None, recurrence_end_date=None)`
  - `schedule_update(client, id, ...)`
  - `media_list(client, media_type=None, status=None, page=None, size=None)`
  - `media_get(client, id)`
  - `media_create(client, title, media_type, description=None, last_watched_at=None)`
  - `media_update(client, id, ...)`

- [ ] **Step 1: 写失败测试（拼装烟雾）**

```python
# python/agent/tests/test_schedule_media_http.py
import unittest
from unittest.mock import AsyncMock, MagicMock

from life_assistant_agent.tools import media as media_tools
from life_assistant_agent.tools import schedule as schedule_tools


def _client():
    c = MagicMock()
    c.get = AsyncMock(return_value=[])
    c.post = AsyncMock(return_value={"id": "1"})
    c.patch = AsyncMock(return_value={"id": "1"})
    c.post_form = AsyncMock(return_value={"id": "m1"})
    c.patch_form = AsyncMock(return_value={"id": "m1"})
    return c


class ScheduleMediaHttpTest(unittest.IsolatedAsyncioTestCase):
    async def test_schedule_list_me(self):
        c = _client()
        await schedule_tools.schedule_list(c, "2026-07-01T00:00:00", "2026-07-07T23:59:59", "me")
        c.get.assert_awaited_with(
            "/schedule/events",
            {"from": "2026-07-01T00:00:00", "to": "2026-07-07T23:59:59"},
        )

    async def test_schedule_list_partner(self):
        c = _client()
        await schedule_tools.schedule_list(c, "2026-07-01T00:00:00", "2026-07-07T23:59:59", "partner")
        self.assertEqual(c.get.await_args.args[0], "/schedule/events/partner")

    async def test_schedule_create_body(self):
        c = _client()
        await schedule_tools.schedule_create(
            c, "约会", "2026-07-20T19:00:00", "2026-07-20T21:00:00", note="饭",
            recurrence="weekly", recurrence_end_date="2026-08-01",
        )
        body = c.post.await_args.args[1]
        self.assertEqual(body["title"], "约会")
        self.assertEqual(body["startAt"], "2026-07-20T19:00:00")
        self.assertEqual(body["endAt"], "2026-07-20T21:00:00")
        self.assertEqual(body["recurrence"], "weekly")
        self.assertEqual(body["recurrenceEndDate"], "2026-08-01")

    async def test_media_create_form(self):
        c = _client()
        await media_tools.media_create(c, "片", "movie", description="好看")
        c.post_form.assert_awaited_with(
            "/shared-media",
            {
                "title": "片",
                "mediaType": "movie",
                "description": "好看",
                "lastWatchedAt": None,
            },
        )

    async def test_media_list_params(self):
        c = _client()
        await media_tools.media_list(c, media_type="tv", status="unfinished", page=2, size=10)
        c.get.assert_awaited_with(
            "/shared-media",
            {"mediaType": "tv", "status": "unfinished", "page": "2", "size": "10"},
        )


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 跑测试确认失败**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_schedule_media_http -v
```

Expected: FAIL（import / attribute 错误）

- [ ] **Step 3: 实现 schedule.py**

```python
# python/agent/src/life_assistant_agent/tools/schedule.py
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
```

- [ ] **Step 4: 实现 media.py**

```python
# python/agent/src/life_assistant_agent/tools/media.py
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
```

- [ ] **Step 5: 跑测试确认通过**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_schedule_media_http -v
```

Expected: PASS

- [ ] **Step 6: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/tools/schedule.py" "python/agent/src/life_assistant_agent/tools/media.py" "python/agent/tests/test_schedule_media_http.py"
git commit -m "feat(agent): add schedule and media HTTP helpers"
```

---

### Task 4: 六域 dispatch()

**Files:**
- Modify: `python/agent/src/life_assistant_agent/tools/todo.py`
- Modify: `python/agent/src/life_assistant_agent/tools/record.py`
- Modify: `python/agent/src/life_assistant_agent/tools/points.py`
- Modify: `python/agent/src/life_assistant_agent/tools/checkin.py`
- Modify: `python/agent/src/life_assistant_agent/tools/schedule.py`
- Modify: `python/agent/src/life_assistant_agent/tools/media.py`
- Create: `python/agent/tests/test_domain_dispatch.py`

**Interfaces:**
- Consumes: Task 1 helpers；各域现有/新建 HTTP 函数
- Produces: 每域 `async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any`
  - 非法 action / 缺字段 → 错误 dict
  - 合法 → HTTP 结果
  - `schedule` allowed: `list`, `create`, `update`；`list` 的 `scope` 默认 `"me"`，仅允许 `me`/`partner`
  - `media` allowed: `list`, `get`, `create`, `update`
  - `todo` allowed: `list`, `upcoming`, `get`, `create`, `update`, `toggle`, `acknowledge`
  - `record` allowed: `list`, `get`, `create`, `update`
  - `points` allowed: `get`, `history`, `change`
  - `checkin` allowed: `do`, `today`, `weekly`

- [ ] **Step 1: 写失败测试**

```python
# python/agent/tests/test_domain_dispatch.py
import unittest
from unittest.mock import AsyncMock, MagicMock

from life_assistant_agent.tools import checkin, media, points, record, schedule, todo


def _client():
    c = MagicMock()
    c.get = AsyncMock(return_value={"ok": True})
    c.post = AsyncMock(return_value={"ok": True})
    c.patch = AsyncMock(return_value={"ok": True})
    c.post_form = AsyncMock(return_value={"ok": True})
    c.patch_form = AsyncMock(return_value={"ok": True})
    return c


class DomainDispatchTest(unittest.IsolatedAsyncioTestCase):
    async def test_todo_invalid_action(self):
        r = await todo.dispatch(_client(), "delete")
        self.assertEqual(r["error"], "invalid_action")
        self.assertIn("list", r["allowed"])

    async def test_todo_missing_id_on_get(self):
        r = await todo.dispatch(_client(), "get")
        self.assertEqual(r, {"error": "missing_field", "field": "id"})

    async def test_todo_list_calls_http(self):
        c = _client()
        await todo.dispatch(c, "list", is_completed=False)
        c.get.assert_awaited()

    async def test_schedule_list_requires_from_to(self):
        r = await schedule.dispatch(_client(), "list", from_="2026-07-01")
        self.assertEqual(r["field"], "to")

    async def test_schedule_bad_scope(self):
        r = await schedule.dispatch(
            _client(), "list",
            from_="2026-07-01T00:00:00", to="2026-07-02T00:00:00", scope="all",
        )
        self.assertEqual(r["error"], "invalid_scope")

    async def test_media_create_requires_media_type(self):
        r = await media.dispatch(_client(), "create", title="x")
        self.assertEqual(r, {"error": "missing_field", "field": "media_type"})

    async def test_points_change_requires_reason(self):
        r = await points.dispatch(_client(), "change", points_change=1)
        self.assertEqual(r["field"], "reason")

    async def test_checkin_do(self):
        c = _client()
        await checkin.dispatch(c, "do", checkin_type="wake")
        c.post.assert_awaited()

    async def test_record_get(self):
        c = _client()
        await record.dispatch(c, "get", id="r1")
        c.get.assert_awaited_with("/shared-records/r1")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 跑测试确认失败**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_domain_dispatch -v
```

Expected: FAIL（无 `dispatch`）

- [ ] **Step 3: 各域追加 dispatch**

在每个 `tools/*.py` 末尾增加 `dispatch`。字段名与 MCP 工具参数一致（snake_case）。`schedule.list` 用 `from_` 接收（Python 关键字），server 层将参数名 `from` 映射为 `from_`（见 Task 5）。

**todo.py 末尾：**

```python
from ..dispatch_util import invalid_action, require

_TODO_ACTIONS = ["list", "upcoming", "get", "create", "update", "toggle", "acknowledge"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _TODO_ACTIONS:
        return invalid_action(_TODO_ACTIONS)
    if action == "list":
        return await todo_list(
            client,
            fields.get("is_completed"),
            fields.get("priority"),
            fields.get("start_due_date"),
            fields.get("end_due_date"),
        )
    if action == "upcoming":
        return await todo_upcoming(client)
    if action == "get":
        err = require(fields, "id")
        return err or await todo_get(client, fields["id"])
    if action == "create":
        err = require(fields, "title")
        return err or await todo_create(
            client,
            fields["title"],
            fields.get("description"),
            fields.get("priority"),
            fields.get("due_date"),
            fields.get("assign_to_partner"),
        )
    if action == "update":
        err = require(fields, "id")
        return err or await todo_update(
            client,
            fields["id"],
            fields.get("title"),
            fields.get("description"),
            fields.get("priority"),
            fields.get("due_date"),
        )
    if action == "toggle":
        err = require(fields, "id")
        return err or await todo_toggle(client, fields["id"])
    if action == "acknowledge":
        err = require(fields, "id")
        return err or await todo_acknowledge(client, fields["id"], fields.get("message"))
    return invalid_action(_TODO_ACTIONS)
```

**record.py 末尾：** 同模式，`list/get/create/update`，`list` 用 `start`/`end`（与现 `record_list` 一致）。

**points.py 末尾：**

```python
_POINTS_ACTIONS = ["get", "history", "change"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _POINTS_ACTIONS:
        return invalid_action(_POINTS_ACTIONS)
    if action == "get":
        return await points_get(client)
    if action == "history":
        return await points_history(client, fields.get("page"), fields.get("size"))
    if action == "change":
        err = require(fields, "points_change", "reason")
        return err or await points_change(client, fields["points_change"], fields["reason"])
    return invalid_action(_POINTS_ACTIONS)
```

注意：`require` 对 `points_change=0` 必须放行（0 是合法扣加边界）。`require` 当前只拒 `None`/`""`，整数 0 可通过——保持如此，不要改成 falsy 检查。

**checkin.py 末尾：** `do` 必填 `checkin_type`；`today`/`weekly` 无参。

**schedule.py 末尾：**

```python
_SCHEDULE_ACTIONS = ["list", "create", "update"]


async def dispatch(client: JavaClient, action: str, **fields: Any) -> Any:
    if action not in _SCHEDULE_ACTIONS:
        return invalid_action(_SCHEDULE_ACTIONS)
    if action == "list":
        err = require(fields, "from_", "to")
        if err:
            return err
        scope = fields.get("scope") or "me"
        if scope not in ("me", "partner"):
            return {"error": "invalid_scope", "allowed": ["me", "partner"]}
        return await schedule_list(client, fields["from_"], fields["to"], scope)
    if action == "create":
        err = require(fields, "title", "start_at", "end_at")
        return err or await schedule_create(
            client,
            fields["title"],
            fields["start_at"],
            fields["end_at"],
            fields.get("note"),
            fields.get("recurrence"),
            fields.get("recurrence_end_date"),
        )
    if action == "update":
        err = require(fields, "id")
        return err or await schedule_update(
            client,
            fields["id"],
            fields.get("title"),
            fields.get("start_at"),
            fields.get("end_at"),
            fields.get("note"),
            fields.get("recurrence"),
            fields.get("recurrence_end_date"),
        )
    return invalid_action(_SCHEDULE_ACTIONS)
```

**media.py 末尾：** `list/get/create/update`；`create` 必填 `title`+`media_type`；`get`/`update` 必填 `id`。

- [ ] **Step 4: 跑测试确认通过**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -m unittest tests.test_domain_dispatch -v
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/tools/todo.py" "python/agent/src/life_assistant_agent/tools/record.py" "python/agent/src/life_assistant_agent/tools/points.py" "python/agent/src/life_assistant_agent/tools/checkin.py" "python/agent/src/life_assistant_agent/tools/schedule.py" "python/agent/src/life_assistant_agent/tools/media.py" "python/agent/tests/test_domain_dispatch.py"
git commit -m "feat(agent): add per-domain MCP action dispatch"
```

---

### Task 5: server.py 改为 6 个域工具

**Files:**
- Modify: `python/agent/src/life_assistant_agent/server.py`（重写工具注册段；保留 AuthASGIWrapper / main）

**Interfaces:**
- Consumes: 各域 `dispatch(client, action, **fields)`
- Produces: 六个 `@mcp.tool` 函数：`todo` / `record` / `schedule` / `media` / `points` / `checkin`
- MCP 参数名：`schedule` 的区间起点对外叫 `from`（合法），内部调用 `dispatch(..., from_=from)`
  （若 FastMCP/Python 不允许参数名 `from`，则对外用 `from_time`/`to_time`，并在 tool description 写清；优先尝试 `from_` 作为公开参数名并在 description 写「区间起点 from_」。**实现时以能注册成功为准：公开参数用 `from_` / `to`。**）

- [ ] **Step 1: 删除全部旧 `@mcp.tool` 函数**（`todo_create` … `checkin_weekly`）

- [ ] **Step 2: 增加共享运行包装**

```python
async def _run(domain_dispatch, action: str, **fields) -> str:
    client = _get_client()
    try:
        result = await domain_dispatch(client, action, **fields)
        return json.dumps(result, ensure_ascii=False)
    finally:
        await client.close()
```

- [ ] **Step 3: 注册 6 个工具（完整签名示例）**

```python
from .tools import checkin as checkin_tools
from .tools import media as media_tools
from .tools import points as points_tools
from .tools import record as record_tools
from .tools import schedule as schedule_tools
from .tools import todo as todo_tools


@mcp.tool(description="待办：action=list|upcoming|get|create|update|toggle|acknowledge")
async def todo(
    action: str,
    id: str | None = None,
    title: str | None = None,
    description: str | None = None,
    priority: str | None = None,
    due_date: str | None = None,
    assign_to_partner: bool | None = None,
    is_completed: bool | None = None,
    start_due_date: str | None = None,
    end_due_date: str | None = None,
    message: str | None = None,
) -> str:
    return await _run(
        todo_tools.dispatch, action,
        id=id, title=title, description=description, priority=priority,
        due_date=due_date, assign_to_partner=assign_to_partner,
        is_completed=is_completed, start_due_date=start_due_date,
        end_due_date=end_due_date, message=message,
    )


@mcp.tool(description="一起做过的事（不是看过的媒体）：action=list|get|create|update")
async def record(
    action: str,
    id: str | None = None,
    title: str | None = None,
    content: str | None = None,
    start: str | None = None,
    end: str | None = None,
    occurred_at: str | None = None,
) -> str:
    return await _run(
        record_tools.dispatch, action,
        id=id, title=title, content=content, start=start, end=end, occurred_at=occurred_at,
    )


@mcp.tool(description="日程：action=list|create|update；list 时 scope=me|partner")
async def schedule(
    action: str,
    from_: str | None = None,
    to: str | None = None,
    scope: str | None = None,
    id: str | None = None,
    title: str | None = None,
    start_at: str | None = None,
    end_at: str | None = None,
    note: str | None = None,
    recurrence: str | None = None,
    recurrence_end_date: str | None = None,
) -> str:
    """日程（独立时间块，非待办）。

    Args:
        action: list / create / update
        from_: list 区间起点，如 2026-07-01 或 2026-07-01T00:00:00
        to: list 区间终点
        scope: me（默认）或 partner
        id: update 时必填
        title, start_at, end_at: create 必填；update 可选
        note, recurrence, recurrence_end_date: 可选；recurrence=none|daily|weekly
    """
    return await _run(
        schedule_tools.dispatch, action,
        from_=from_, to=to, scope=scope, id=id, title=title,
        start_at=start_at, end_at=end_at, note=note,
        recurrence=recurrence, recurrence_end_date=recurrence_end_date,
    )


@mcp.tool(description="一起看过的内容（电影/书/剧，不是做过的事）：action=list|get|create|update；无封面上传")
async def media(
    action: str,
    id: str | None = None,
    title: str | None = None,
    media_type: str | None = None,
    description: str | None = None,
    last_watched_at: str | None = None,
    is_finished: bool | None = None,
    status: str | None = None,
    page: int | None = None,
    size: int | None = None,
) -> str:
    return await _run(
        media_tools.dispatch, action,
        id=id, title=title, media_type=media_type, description=description,
        last_watched_at=last_watched_at, is_finished=is_finished,
        status=status, page=page, size=size,
    )


@mcp.tool(description="伴侣积分：action=get|history|change")
async def points(
    action: str,
    page: int | None = None,
    size: int | None = None,
    points_change: int | None = None,
    reason: str | None = None,
) -> str:
    return await _run(
        points_tools.dispatch, action,
        page=page, size=size, points_change=points_change, reason=reason,
    )


@mcp.tool(description="作息打卡：action=do|today|weekly；do 时 checkin_type=wake|sleep")
async def checkin(
    action: str,
    checkin_type: str | None = None,
) -> str:
    return await _run(checkin_tools.dispatch, action, checkin_type=checkin_type)
```

- [ ] **Step 4: 语法/导入冒烟**

```powershell
cd "d:\life_assistant\python\agent"
$env:PYTHONPATH = "src"
python -c "from life_assistant_agent.server import todo, record, schedule, media, points, checkin; print('ok', todo.__name__, schedule.__name__)"
python -m unittest discover -s tests -v
```

Expected: 打印 `ok todo schedule`；全部 unittest PASS。确认 `dir`/`grep` 下 `server.py` **没有** `todo_create` / `todo_list` 等旧名。

- [ ] **Step 5: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/server.py"
git commit -m "feat(agent): collapse MCP tools into 6 domain routers"
```

---

### Task 6: 文档同步

**Files:**
- Modify: `docs/superpowers/specs/2026-06-28-mcp-agent-module-design.md`（工具表改为指向域路由；注明已被 `2026-07-19-mcp-domain-router-design.md` 取代工具面）

- [ ] **Step 1: 在旧 agent module design 顶部或工具节增加**

```markdown
> **工具面更新（2026-07-19）：** 细粒度工具已收束为 6 个域路由工具。
> 见 `docs/superpowers/specs/2026-07-19-mcp-domain-router-design.md`。
> 工具：`todo` / `record` / `schedule` / `media` / `points` / `checkin`（各带 `action`）。
```

删除或划掉文中「11 个工具」清单中与现状冲突的部分（保留架构/认证段落）。

- [ ] **Step 2: Commit**

```powershell
git add "docs/superpowers/specs/2026-06-28-mcp-agent-module-design.md"
git commit -m "docs: point MCP agent module design at domain-router tools"
```

---

## 手动验证（实现完成后）

1. 启动 Agent，用 MCP 客户端列出工具 → 恰好 6 个。
2. `schedule` `action=list` + `from_`/`to`/`scope=partner` → 返回伴侣事件或空列表。
3. `media` `action=create`（无封面）→ Java 创建成功。
4. `todo` `action=list` → 与旧 `todo_list` 行为等价。
5. 非法 `action` → JSON 含 `invalid_action`。

---

## Spec coverage（自检）

| Spec 要求 | Task |
|-----------|------|
| 6 域工具 | 5 |
| schedule list/create/update + partner | 3, 4, 5 |
| media list/get/create/update，无 progress/封面 | 3, 4, 5 |
| todo/record/points/checkin 能力保留 | 4, 5 |
| breaking 删除旧名 | 5 |
| 结构化错误 JSON | 1, 4 |
| form client for media | 2, 3 |
| 最小测试 | 1–4 |
| 文档更新 | 6 |
