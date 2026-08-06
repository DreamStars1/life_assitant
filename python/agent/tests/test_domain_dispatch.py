import unittest
from unittest.mock import AsyncMock, MagicMock

from life_assistant_agent.tools import checkin, media, points, record, schedule, todo


def _client():
    c = MagicMock()
    c.get = AsyncMock(return_value={"ok": True})
    c.post = AsyncMock(return_value={"ok": True})
    c.put = AsyncMock(return_value={"ok": True})
    c.patch = AsyncMock(return_value={"ok": True})
    c.delete = AsyncMock(return_value={"ok": True})
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

    async def test_health_meal_upsert_requires_fields(self):
        from life_assistant_agent.tools import health as health_tools
        c = _client()
        out = await health_tools.dispatch(c, "meal_upsert", date="2026-08-03")
        self.assertEqual(out["error"], "missing_field")

    async def test_health_meal_upsert_ok(self):
        from life_assistant_agent.tools import health as health_tools
        c = _client()
        await health_tools.dispatch(
            c, "meal_upsert",
            date="2026-08-03", meal_type="早餐", food="鸡蛋", protein_g=7, kcal=150,
        )
        c.put.assert_awaited_with(
            "/health/meals",
            {
                "date": "2026-08-03",
                "mealType": "早餐",
                "food": "鸡蛋",
                "proteinG": 7,
                "kcal": 150,
            },
        )

    async def test_health_daily_update_burn_kcal(self):
        from life_assistant_agent.tools import health as health_tools
        c = _client()
        await health_tools.dispatch(
            c, "daily_update",
            date="2026-08-06", burn_kcal=1800,
        )
        c.put.assert_awaited_with(
            "/health/daily",
            {"date": "2026-08-06", "burnKcal": 1800},
        )

    async def test_health_daily_update_clear_burn_kcal(self):
        from life_assistant_agent.tools import health as health_tools
        c = _client()
        await health_tools.dispatch(
            c, "daily_update",
            date="2026-08-06", burn_kcal=None,
        )
        c.put.assert_awaited_with(
            "/health/daily",
            {"date": "2026-08-06", "burnKcal": None},
        )

    async def test_health_invalid_action(self):
        from life_assistant_agent.tools import health as health_tools
        c = _client()
        out = await health_tools.dispatch(c, "nope")
        self.assertEqual(out["error"], "invalid_action")
        self.assertIn("meal_upsert", out["allowed"])


if __name__ == "__main__":
    unittest.main()
