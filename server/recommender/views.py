"""HTTP endpoints for the recommender app.

Two endpoints, both per-user-keyed by an opaque ID supplied by the Android
client (a v4 UUID generated at install time):

* ``GET  /recommender/SendUserFile/<id>`` — stream the user's recommendation
  SQLite back to the client. New users get a copy of the default DB.
* ``POST /recommender/GetUserFile/<id>`` — receive the user's reading-history
  SQLite, then enqueue a Celery task to recompute their profile.

Both are HMAC-signed (see :mod:`recommender.auth`) and the user ID is
strictly validated and resolved against ``USERPROFILE_ROOT`` to prevent
path traversal.
"""

import re
import shutil
from pathlib import Path

from django.conf import settings
from django.http import (
    FileResponse,
    HttpResponse,
    HttpResponseBadRequest,
    HttpResponseServerError,
)
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from .auth import require_signed_request
from .forms import UploadFileForm
from .tasks import Run_User_Update


# Keep the per-user ID to a safe character set so it can never escape the
# userprofile directory via `..`, `/`, NULs, etc. ANDROID_ID / UUIDs both fit.
_ID_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")

USERPROFILE_ROOT = (Path(settings.BASE_DIR) / "recommender" / "userprofile").resolve()
DEFAULT_RECOMMENDDB = USERPROFILE_ROOT / "default" / "recommenddb"


def _user_dir(user_id: str) -> Path:
    """Resolve and validate the per-user directory under USERPROFILE_ROOT."""
    if not _ID_RE.match(user_id):
        raise ValueError("invalid user id")
    candidate = (USERPROFILE_ROOT / user_id).resolve()
    # Defense in depth: even with the regex, confirm the resolved path is
    # rooted under USERPROFILE_ROOT before any I/O.
    if not candidate.is_relative_to(USERPROFILE_ROOT):
        raise ValueError("invalid user id")
    return candidate


@require_http_methods(["GET"])
@require_signed_request
def SendUserFile(request, ID):
    """Stream the per-user recommenddb SQLite back to the Android client."""
    try:
        user_dir = _user_dir(ID)
    except ValueError:
        return HttpResponseBadRequest("invalid id")

    target = user_dir / "recommenddb"
    if not target.is_file():
        # Cold path: brand-new user. Materialise the default DB once.
        if not DEFAULT_RECOMMENDDB.is_file():
            return HttpResponseServerError("default recommenddb missing")
        user_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(DEFAULT_RECOMMENDDB, target)

    response = FileResponse(target.open("rb"), content_type="application/octet-stream")
    response["Content-Disposition"] = 'attachment; filename="recommenddb"'
    return response


@csrf_exempt  # Mobile client uploads do not carry a CSRF cookie.
@require_http_methods(["POST"])
@require_signed_request
def GetUserFile(request, ID):
    """Receive the user's reading-history SQLite, then kick off recompute."""
    try:
        user_dir = _user_dir(ID)
    except ValueError:
        return HttpResponseBadRequest("invalid id")

    form = UploadFileForm(request.POST, request.FILES)
    if not form.is_valid():
        return HttpResponseBadRequest("invalid upload")

    user_dir.mkdir(parents=True, exist_ok=True)
    try:
        _write_upload(request.FILES["file"], user_dir / "recorddb")
    except ValueError:
        # The streaming cap was exceeded mid-write.
        return HttpResponseBadRequest("file too large")

    Run_User_Update.delay(ID)
    return HttpResponse("success")


def _write_upload(uploaded_file, dest: Path) -> None:
    """Stream the upload to disk in chunks, capped at MAX_UPLOAD_BYTES."""
    written = 0
    cap = settings.MAX_UPLOAD_BYTES
    try:
        with open(dest, "wb") as fh:
            for chunk in uploaded_file.chunks():
                written += len(chunk)
                if written > cap:
                    raise ValueError("upload exceeded size cap")
                fh.write(chunk)
    except ValueError:
        # Don't leave a half-written file lying around for the recompute task.
        dest.unlink(missing_ok=True)
        raise
