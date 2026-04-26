"""HMAC request signing.

Goal: a client that knows the per-install user ID alone cannot impersonate
another user. Requests must also be signed with a shared secret baked into
the client at build time, and timestamped so signatures can't be replayed
indefinitely.

This is not a substitute for real authentication (the secret is in the APK
and can be extracted), but it raises the bar from "trivial" to "needs to
reverse engineer the app", which is enough for a hobby/portfolio project.
"""

from __future__ import annotations

import hashlib
import hmac
import os
import time
from functools import wraps

from django.conf import settings
from django.http import HttpResponseForbidden

# Skew window: signatures older than this are rejected. 5 minutes covers
# clock drift on phones without making replay attacks easy.
MAX_SKEW_SECONDS = 5 * 60

SHARED_SECRET = os.environ.get("REQUEST_SIGNING_SECRET", "").encode("utf-8")


def _expected_signature(method: str, path: str, timestamp: str) -> str:
    # The canonical payload MUST match the client byte-for-byte
    # (see Android RequestSigner.sign). The path comes from request.path,
    # which omits the query string and trailing slashes Django did not add
    # itself. If you ever start signing query parameters or the request body,
    # update *both* sides in lockstep or signatures will silently 403.
    msg = f"{method.upper()}\n{path}\n{timestamp}".encode("utf-8")
    return hmac.new(SHARED_SECRET, msg, hashlib.sha256).hexdigest()


def require_signed_request(view_func):
    """Reject requests without a valid X-Signature/X-Timestamp pair."""

    @wraps(view_func)
    def _wrapped(request, *args, **kwargs):
        if not SHARED_SECRET:
            # Allow bypass in DEBUG when no secret is configured so local
            # development works without ceremony. Production must set the env
            # var or every request 403s — fail loud rather than fail open.
            if settings.DEBUG:
                return view_func(request, *args, **kwargs)
            return HttpResponseForbidden("server signing key not configured")

        sig = request.headers.get("X-Signature", "")
        ts = request.headers.get("X-Timestamp", "")
        if not sig or not ts:
            return HttpResponseForbidden("missing signature")

        try:
            ts_int = int(ts)
        except ValueError:
            return HttpResponseForbidden("bad timestamp")

        if abs(time.time() - ts_int) > MAX_SKEW_SECONDS:
            return HttpResponseForbidden("stale request")

        expected = _expected_signature(request.method, request.path, ts)
        if not hmac.compare_digest(expected, sig):
            return HttpResponseForbidden("bad signature")

        return view_func(request, *args, **kwargs)

    return _wrapped
