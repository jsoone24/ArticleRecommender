# Article Recommender

Personalized article recommendation Android app — content-based recommender
backed by a Django server that crawls Naver News, extracts keyword vectors,
and ranks articles by cosine similarity against a user profile built from
their reading history.

## Architecture

```
┌────────────────┐      HTTPS + HMAC      ┌──────────────────┐
│ Android client │ ─────────────────────▶ │  Django server   │
│  (Java)        │ ◀───────────────────── │  + Celery worker │
└────────────────┘                        └──────────────────┘
                                                  │
                                                  ▼
                                         crawler → SQLite/CSV
```

The client uploads a SQLite file of reading history when the app closes; the
server triggers a Celery task that updates the user keyword profile and
recomputes recommendations, then the client downloads the resulting SQLite
on next launch.

## Server setup

```bash
cd server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env
# Then edit .env and fill in:
#   DJANGO_SECRET_KEY=$(python -c "import secrets; print(secrets.token_urlsafe(50))")
#   REQUEST_SIGNING_SECRET=$(python -c "import secrets; print(secrets.token_urlsafe(32))")
#   DJANGO_ALLOWED_HOSTS=your.domain.tld
#   DJANGO_DEBUG=False

# Standard Django bootstrap
python manage.py migrate
python manage.py check

# Run with the env file loaded — `set -a` exports every var defined in .env.
set -a; source .env; set +a
python manage.py runserver 0.0.0.0:8080
# In a second shell, start the Celery worker:
celery -A django_project worker -l info
```

For production, put it behind a TLS-terminating reverse proxy (nginx +
Let's Encrypt) — the Android client refuses cleartext.

### Key files

| Path | Role |
|---|---|
| `server/crawler/crawlling/crolling.py` | Crawls Naver News |
| `server/crawler/crawlling/Make_DB.py` | Standardizes crawled data into keyword vectors |
| `server/recommender/py/User_update.py` | Updates per-user keyword profile from reading history |
| `server/recommender/py/DB_similarity.py` | Cosine similarity ranking |
| `server/recommender/views.py` | HTTP endpoints (with size cap, ID validation, HMAC auth) |
| `server/recommender/auth.py` | HMAC-SHA256 request signing |

## Android setup

Toolchain: **AGP 8.7.3, Gradle 8.9, compileSdk 36, JDK 17+** (the bundled JBR
21 in Android Studio works out of the box).

Edit `androidApp/gradle.properties` (or pass via `-P` on the gradle command line):

```properties
SERVER_BASE_URL=https://your-server.example.com
REQUEST_SIGNING_SECRET=<must-match-server>
```

Then either:

```bash
cd androidApp
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

…or open the project in Android Studio. If gradle sync complains about the
JDK version, set **Settings → Build, Execution, Deployment → Build Tools →
Gradle → Gradle JDK** to the bundled `jbr-21`.

The signing secret is exposed via `BuildConfig.REQUEST_SIGNING_SECRET` at
compile time — never hand-coded into sources.

Min SDK 23, target SDK 36.

## Security model

- All traffic is HTTPS; the network security config rejects cleartext.
- Per-install user IDs are random v4 UUIDs stored in `SharedPreferences`,
  not `ANDROID_ID`.
- Every request carries an HMAC-SHA256 signature over `METHOD\nPATH\nTIMESTAMP`,
  verified server-side. Stale timestamps (>5 min skew) are rejected.
- User-supplied IDs are validated against `^[A-Za-z0-9_-]{1,64}$` and the
  resolved file path is checked against the userprofile root.
- Uploaded files are streamed to disk with a 5 MB cap.
- Local SQLite is excluded from `adb backup` (`allowBackup="false"`).

The HMAC secret is in the APK, so a determined attacker can extract it.
This is a hobby project — for stronger guarantees, swap in OAuth2 or
device-attested tokens.

## Known limitations

- Recommendations are content-based on keyword cosine similarity only — no
  collaborative signal, no diversity re-ranking.
- Crawler is hardcoded for Naver News (Korean).
- The `recorddb` SQLite is shipped between client and server as the API
  contract; a JSON REST API would be cleaner.

<p align="right"><br/>-First and Last CIY Project-</p>
