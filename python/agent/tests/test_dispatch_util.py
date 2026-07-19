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
