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
