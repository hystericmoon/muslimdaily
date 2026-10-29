"""The release body comes from the app's What's new, in plain text for the in-app updater."""
import unittest

from release_notes import notes


class ReleaseNotesTest(unittest.TestCase):
    def test_both_languages_as_plain_text(self):
        text = notes("1.4.1")
        self.assertTrue(text.startswith("Yang baru:\n- "))
        self.assertIn("\n\nWhat's new:\n- ", text)
        self.assertNotIn("#", text)
        self.assertNotIn("\\'", text)

    def test_unknown_version_is_empty(self):
        self.assertEqual(notes("0.0.0"), "")
