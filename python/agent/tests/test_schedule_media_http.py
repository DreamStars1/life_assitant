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
