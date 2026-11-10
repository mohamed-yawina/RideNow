"""Security regression tests for RideNow's JWT helpers.

Run from Backend/: python -m unittest discover -s tests -v
Requires python-jose, passlib, bcrypt and python-dotenv.
"""
import importlib
import os
import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path
from unittest.mock import patch

BACKEND = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND))

TEST_SECRET = "test-only-ridenow-secret-please-do-not-use-in-production-12345"


class AuthenticationSecurityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # The application intentionally requires a configured secret.
        with patch.dict(os.environ, {"SECRET_KEY": TEST_SECRET}):
            cls.auth = importlib.import_module("auth")

    def test_valid_token_round_trip(self):
        token = self.auth.create_access_token({
            "sub": "test-user-1", "role": "client", "email": "test@example.invalid"
        })
        payload = self.auth.decode_token(token)
        self.assertEqual(payload["sub"], "test-user-1")
        self.assertEqual(payload["role"], "client")
        self.assertGreater(payload["exp"], int(datetime.now(timezone.utc).timestamp()))

    def test_invalid_token_is_rejected(self):
        self.assertIsNone(self.auth.decode_token("not-a-valid-jwt"))

    def test_token_signed_with_other_key_is_rejected(self):
        from jose import jwt
        forged = jwt.encode({"sub": "other-user"}, "different-signing-key", algorithm="HS256")
        self.assertIsNone(self.auth.decode_token(forged))

    def test_expired_token_is_rejected(self):
        from jose import jwt
        expired = jwt.encode(
            {"sub": "test-user-1", "exp": 1},
            TEST_SECRET,
            algorithm="HS256",
        )
        self.assertIsNone(self.auth.decode_token(expired))

    def test_password_hash_round_trip(self):
        password = "correct-horse-battery-staple"
        hashed = self.auth.get_password_hash(password)
        self.assertNotEqual(password, hashed)
        self.assertTrue(self.auth.verify_password(password, hashed))
        self.assertFalse(self.auth.verify_password("incorrect-password", hashed))


if __name__ == "__main__":
    unittest.main()
