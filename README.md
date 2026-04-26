# Article Recommender

A personalized news-article recommendation Android app, backed by a Django
server that crawls Naver News, extracts keyword vectors, and ranks articles
by cosine similarity against a per-user keyword profile built from reading
history.

Built originally as a college capstone; modernized in 2026 with current
tooling (AGP 8.7, Gradle 8.9, Realm 10.19, Django 4.2+) and security
hardening (HMAC request signing, HTTPS-only, path-traversal defenses).

---

## Table of contents

1. [Architecture](#architecture)
2. [How the recommender works](#how-the-recommender-works)
3. [Request lifecycle](#request-lifecycle)
4. [Data schemas](#data-schemas)
5. [Server setup](#server-setup)
6. [Android setup](#android-setup)
7. [Security model](#security-model)
8. [File map](#file-map)
9. [Known limitations](#known-limitations)

---

## Architecture

```
┌──────────────────────────┐                       ┌────────────────────────┐
│      Android client      │   HTTPS + HMAC-SHA256 │     Django server      │
│         (Java)           │ ◀───────────────────▶ │                        │
│                          │                       │  ┌──────────────────┐  │
│  ┌────────────────────┐  │   GET  recommenddb    │  │  views.py        │  │
│  │ StartScreen        │  │ ─────────────────────▶│  │  (HTTP gateway)  │  │
│  │  └─▶ MainActivity  │  │ ◀───────────────────  │  └────────┬─────────┘  │
│  │       ├ Articles   │  │   POST recorddb       │           │            │
│  │       ├ Bookmarks  │  │ ─────────────────────▶│           ▼            │
│  │       └ User       │  │                       │  ┌──────────────────┐  │
│  └────────────────────┘  │                       │  │  Celery worker   │  │
│                          │                       │  │  (User_update +  │  │
│  Local storage:          │                       │  │   DB_similarity) │  │
│   • SQLite recorddb      │                       │  └────────┬─────────┘  │
│   • SQLite recommenddb   │                       │           │            │
│   • Realm  bookmarks     │                       │           ▼            │
└──────────────────────────┘                       │  ┌──────────────────┐  │
                                                   │  │   Naver News     │  │
                                                   │  │   crawler        │  │
                                                   │  │   (offline cron) │  │
                                                   │  └──────────────────┘  │
                                                   └────────────────────────┘
```

The client uploads a SQLite file of reading history when the app closes; the
server triggers a Celery task that updates the user's keyword profile and
recomputes their personal article ranking, then the client downloads the
resulting SQLite on next launch.

---

## How the recommender works

A pure **content-based recommender**: each article is reduced to a sparse
keyword-weight vector at crawl time, each user is represented as a single
keyword-weight vector built from the articles they've read, and ranking is
the cosine similarity between the user vector and every article vector.

### Pipeline

```
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ 1. Crawl     │──▶│ 2. Pivot &   │──▶│ 3. Update    │──▶│ 4. Score &   │
│   Naver News │   │   normalise  │   │   user vec   │   │   rank       │
│              │   │   keywords   │   │              │   │              │
│   crolling   │   │   Make_DB    │   │  User_update │   │ DB_similarity│
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
   per-article         articleDB          UserData         recommenddb
   CSV per             pivoted CSV        CSV per          SQLite per
   sub-category        per day            user             user
```

**Step 1 — Crawl** (`crawler/crawlling/crolling.py`)
Walks Naver News across six top-level categories (politics, economy, society,
life/culture, world, IT/science) and ~50 sub-categories. For each article it
uses [`newspaper3k`](https://newspaper.readthedocs.io/) for body extraction
and [`gensim`](https://radimrehurek.com/gensim/) for TextRank-based keyword
extraction. Output: one CSV per `(date, category, sub-category)` with rows
of `aid, oid, sid1, sid2, body, summary, keyword 0..4, keyword N weight`.

**Step 2 — Pivot & normalise** (`crawler/crawlling/Make_DB.py`)
Per day, melts every per-sub-category CSV into a single matrix:
- Rows: articles (uniquely identified by `aid`)
- Columns: keywords, namespaced as `sid1_sid2_<keyword>` so the same surface
  string in different categories doesn't collide
- Cells: TextRank weight (0..1), or 0 if the article doesn't mention that
  keyword

This is the article × keyword sparse matrix the rest of the pipeline uses.

**Step 3 — Update the user vector** (`recommender/py/User_update.py`)
For each newly-read article in the user's `recorddb`:
1. Look up that article's keyword vector in the article DB.
2. Re-weight: `new = old × (article × 100 + 1)` — articles you've read
   *amplify* the keywords they contain in your profile, with a `+1` floor so
   any keyword you've encountered survives even if its weight was tiny.
3. Renormalise so the user's keyword weights sum to 1.
4. Drop the lowest-weight keyword to keep the vector bounded.

The result is a user "interest" vector that drifts toward the keywords of
articles the user actually opens.

**Step 4 — Score & rank** (`recommender/py/DB_similarity.py`)
1. Stack the user vector on top of the article matrix (so they share columns).
2. `sklearn.metrics.pairwise.cosine_similarity` produces the user-vs-articles
   row of the similarity matrix.
3. Sort articles by similarity, take the top 100.
4. Write `(link, similarity)` rows into the user's `recommenddb` SQLite,
   which the client downloads on next launch.

### Why content-based?

- **No cold start across users.** A brand-new user can be served the global
  default vector immediately; their personal vector emerges as they read.
- **Explainable.** The user's profile is a literal list of weighted keywords —
  trivial to debug and to expose in a "why was this recommended?" UI.
- **Self-contained.** No need to share read events between users, which would
  raise privacy and consent questions for a hobby project.

The trade-off is a smaller diversity surface than collaborative filtering
would give — see [known limitations](#known-limitations).

---

## Request lifecycle

```
Android                                 Server                  Celery worker
───────                                 ──────                  ─────────────

App launch
  │
  │  GET /recommender/SendUserFile/<uuid>
  │  X-Timestamp, X-Signature
  │ ───────────────────────────────────▶│
  │                                     │ validate ID + HMAC
  │                                     │ if first-time user:
  │                                     │   copy default recommenddb
  │  ◀──────────────────────── recommenddb (SQLite, ~tens of KB)
  │
  │  scrape article metadata via Jsoup, render list
  │
  ▼
User reads articles
  │
  │  insert (readdate, articledate, link) into local recorddb
  │  toggle bookmarks → Realm
  │
  ▼
App close (onDestroy or onTaskRemoved)
  │
  │  POST /recommender/GetUserFile/<uuid>
  │  multipart upload of recorddb
  │  X-Timestamp, X-Signature
  │ ───────────────────────────────────▶│
  │                                     │ validate ID + HMAC + size cap
  │                                     │ stream upload to disk
  │                                     │ enqueue Run_User_Update.delay(uuid)
  │                                     │                              │
  │  ◀────────────────────── 200 success                              │
  │                                                                    │
  │                                                  User_update.main(uuid)
  │                                                  → drift the user vector
  │                                                                    │
  │                                                  DB_similarity.main(uuid)
  │                                                  → rewrite recommenddb
  │
  ▼
Next launch — fresh recommendations
```

---

## Data schemas

### Server-side, on disk

```
server/recommender/userprofile/
├── default/
│   └── recommenddb         # bootstrapped for new users
└── <user-uuid>/
    ├── recommenddb          # SQLite — what the client downloads
    ├── recorddb             # SQLite — what the client uploads
    └── UserData.csv         # 1×N keyword-weight vector for this user
```

```
server/crawler/crawlling/
├── articleInfo/<date>/<sid1>/sid2_<sid2>.csv   # raw crawl output
└── articleDB/<date>_DB.csv                     # pivoted keyword matrix
```

### `recommenddb` (server → client)

| table  | column     | type   | purpose                                |
|--------|------------|--------|----------------------------------------|
| tblink | index      | INT    | sort order (highest similarity first)  |
| tblink | link       | TEXT   | article URL on news.naver.com          |
| tblink | similarity | REAL   | cosine similarity to the user vector   |

The client reads the top 50 of these on startup, scrapes each article's
title/date/publisher/thumbnail, and renders them in `ArticleFragment`.

### `recorddb` (client → server)

| table     | column      | type | purpose                            |
|-----------|-------------|------|------------------------------------|
| tb_record | _id         | INT  | autoincrement PK                   |
| tb_record | readdate    | TEXT | when the user opened the article   |
| tb_record | articledate | TEXT | publish date of the article        |
| tb_record | link        | TEXT | article URL                        |

Inserted in `ReadMode.onCreate` every time the user opens an article.

### Realm (Android, local)

`BookmarkVO` rows hold the full snapshot of a bookmarked article (title,
link, publisher, date, scraped body, image URI). Lives in the app's private
Realm file; never leaves the device.

---

## Server setup

```bash
cd server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env
# Edit .env and fill in at minimum:
#   DJANGO_SECRET_KEY=$(python -c "import secrets; print(secrets.token_urlsafe(50))")
#   REQUEST_SIGNING_SECRET=$(python -c "import secrets; print(secrets.token_urlsafe(32))")
#   DJANGO_ALLOWED_HOSTS=your.domain.tld
#   DJANGO_DEBUG=False           # True for local dev
```

Bootstrap and run:

```bash
python manage.py migrate
python manage.py check

set -a; source .env; set +a            # export every var defined in .env
python manage.py runserver 0.0.0.0:8080

# In a second shell:
celery -A django_project worker -l info
```

For production, put it behind a TLS-terminating reverse proxy (nginx +
Let's Encrypt or similar) — the Android client refuses cleartext.

To run the crawler manually (the date is currently hard-coded to
2020-09-05; adjust in the source):

```bash
python -m crawler.crawlling.crolling   # raw crawl
python -m crawler.crawlling.Make_DB    # pivot into articleDB/
```

---

## Android setup

Toolchain: **AGP 8.7.3, Gradle 8.9, compileSdk 36, JDK 17+** (the bundled
JBR 21 in Android Studio works out of the box).

Configure `androidApp/gradle.properties` (or pass via `-P` on the gradle
command line):

```properties
SERVER_BASE_URL=https://your-server.example.com
REQUEST_SIGNING_SECRET=<must-match-server>
```

Build:

```bash
cd androidApp
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

…or open the project in Android Studio. If gradle sync complains about the
JDK version, set **Settings → Build, Execution, Deployment → Build Tools →
Gradle → Gradle JDK** to the bundled `jbr-21`.

The signing secret is exposed via `BuildConfig.REQUEST_SIGNING_SECRET` at
compile time — never hand-coded into sources. Release builds with an empty
secret fail at gradle time so unsigned APKs can't ship.

Min SDK 23, target SDK 36.

---

## Security model

| Threat                                  | Mitigation                                                            |
|-----------------------------------------|-----------------------------------------------------------------------|
| Reading history sniffed in transit      | HTTPS-only network policy; `usesCleartextTraffic` removed             |
| Cross-app tracking via `ANDROID_ID`     | Per-install v4 UUID in private SharedPreferences                      |
| Anyone with a UUID impersonates a user  | HMAC-SHA256 signature over `METHOD\nPATH\nTIMESTAMP`, 5-min skew      |
| Path traversal via crafted user ID      | Regex `^[A-Za-z0-9_-]{1,64}$` + `Path.is_relative_to(USERPROFILE_ROOT)` |
| Disk-fill via huge upload               | 5 MB streaming cap; partial files unlinked on overflow                |
| `adb backup` siphoning local SQLite     | `allowBackup="false"` + `fullBackupContent="false"`                   |
| XSS via scraped article HTML            | WebView JavaScript explicitly disabled                                |
| CSRF on web routes                      | `CsrfViewMiddleware` re-enabled; mobile endpoints `csrf_exempt`        |
| Secrets in source                       | All env-driven; `SECRET_KEY` refuses to boot prod with default        |

The HMAC secret is in the APK, so a sufficiently determined attacker can
extract it. This is documented intentionally — for stronger guarantees, the
right next steps are OAuth2 or device-attested tokens, not more obfuscation.

---

## File map

### Server

| Path                                      | Role                                              |
|-------------------------------------------|---------------------------------------------------|
| `server/django_project/settings.py`       | Env-driven Django config; HSTS / cookies in prod  |
| `server/django_project/celery.py`         | Celery app definition                             |
| `server/recommender/views.py`             | HTTP gateway: validation, signing, file I/O       |
| `server/recommender/auth.py`              | HMAC request-signing decorator                    |
| `server/recommender/forms.py`             | Multipart upload validation                       |
| `server/recommender/tasks.py`             | Celery task wiring                                |
| `server/recommender/py/User_update.py`    | Drift the user keyword vector from new reads      |
| `server/recommender/py/DB_similarity.py`  | Cosine-similarity ranking → `recommenddb`         |
| `server/crawler/crawlling/crolling.py`    | Naver News crawler + keyword extraction           |
| `server/crawler/crawlling/Make_DB.py`     | Pivot crawl output into the article × keyword CSV |
| `server/.env.example`                     | Template for required environment variables       |
| `server/requirements.txt`                 | Pinned Python dependencies                        |

### Android

| Path                                                      | Role                                            |
|-----------------------------------------------------------|-------------------------------------------------|
| `androidApp/app/src/main/java/com/example/project/`       |                                                 |
| `Config.java`                                             | Server URL, paths, per-install user ID          |
| `RequestSigner.java`                                      | HMAC client-side signing                        |
| `StartScreen.java`                                        | Splash: download recommendations + scrape meta  |
| `MainActivity.java`                                       | Tab host (Articles / Bookmarks / User)          |
| `ReadMode.java`                                           | Article reader; bookmark toggle; record read    |
| `ArticleFragment.java` / `ArticleAdapter.java`            | Recommended-list UI                             |
| `BookmarkFragment.java` / `BookmarkAdapter.java`          | Bookmarks UI (Realm-backed)                     |
| `UserFragment.java` / `UserRecordView.java`               | User info + reading history viewer              |
| `RecommendRequester.java`                                 | GET the recommend SQLite from the server        |
| `RecordSender.java`                                       | POST the reading-history SQLite to the server   |
| `ForecdTerminationService.java`                           | Catch swipe-kill so the upload still happens    |
| `RecordDBHelper.java`                                     | SQLiteOpenHelper for the local `recorddb`       |
| `*VO.java`                                                | Plain data holders                              |
| `res/xml/networkset.xml`                                  | HTTPS-only network policy                       |

---

## Known limitations

- **Hard-coded crawl date.** `crolling.py`, `Make_DB.py`, `User_update.py`,
  and `DB_similarity.py` all reference `2020-09-05`. To run live, swap those
  for `datetime.date.today()` (or wire a CLI arg) and schedule the crawler
  on a daily cron.
- **Single source.** The crawler is hard-coded to news.naver.com.
- **Content-based only.** No collaborative signal, no diversity re-ranker,
  so the top of the list can become an echo of recent reads. A simple MMR
  pass after `DB_similarity` would help.
- **Keyword extraction is TextRank.** Surface-form matching means
  near-synonyms ("서울", "서울시") don't share weight. A switch to
  multilingual sentence embeddings (e.g. KoSimCSE) would lift recommendation
  quality noticeably.
- **SQLite-as-API.** The client and server exchange `.sqlite` files instead
  of JSON. It works, but a normal REST API would be easier to evolve.
- **No real auth.** HMAC stops impersonation by anyone who only knows a UUID,
  but the shared secret is in the APK. A live deployment should add OAuth2
  or device attestation.

<p align="right"><br/>-First and Last CIY Project-</p>
