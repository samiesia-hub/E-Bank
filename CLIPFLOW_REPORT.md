# ClipFlow — complete report

## File tree

```text
clipflow/
├── .gitignore
├── README.md
├── app.py
├── index.html
├── pytest.ini
├── requirements.txt
├── downloads/          # runtime output (empty; ignored)
└── tests/
    └── test_app.py
```

## How to run

```bash
cd clipflow
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app
```

Open <http://127.0.0.1:8000>.

## Verification

- `python3 -m pytest -q`: 8 passed.
- `GET /api/health`: HTTP 200 with yt-dlp and ffmpeg available.
- Real metadata for `https://www.youtube.com/watch?v=1UaBgr_sq9A`: 13 formats.
- Codec labels: real metadata exposed H.264, VP9, AAC and Opus; the UI rendered the labels and raw codec ids as tooltips.
- Real merged download (format 134): 4,953,073-byte MKV, h264 + opus, ffprobe passed.
- File endpoint: HTTP 200, `video/matroska`, sanitized `Content-Disposition`, correct length.
- Real cancel: job cancelled and job directory removed.
- UI: metadata, format list, real progress, success/download link, cancel, error states; Lighthouse 100/100/100.
- Concurrency: fourth active job returned 429 `too_many_jobs`.
- Missing ffmpeg and missing yt-dlp paths returned the expected diagnostics.

## Known limitations

- In-memory jobs and metadata cache; restart loses job state.
- No auth or rate limiting; do not expose publicly.
- DNS rebinding and yt-dlp redirects are outside pre-validation.
- Private, members-only, age-restricted and bot-checked videos need cookies, which are not collected.
- No transcoding; codec labels are informational only.
- Per-stream progress can restart when the audio stream begins for merged formats.
- No duration/size cap; disk usage is the operator responsibility.
- Single worker only; multiple workers split in-memory state and multiply the job limit.

## Full file contents


### `clipflow/.gitignore`

``````text
__pycache__/
.pytest_cache/
.venv/
downloads/
*.py[cod]

``````

### `clipflow/README.md`

``````text
# ClipFlow

ClipFlow is a single-server YouTube/media downloader. FastAPI serves one static page and wraps `yt-dlp` through subprocess argument lists. There is no build step and no database.

## Requirements

- Python 3.10+
- `yt-dlp` (installed by `requirements.txt`, or available on `PATH`)
- `ffmpeg` on `PATH` for video-only formats (they are merged with the best audio)

## How to run

```bash
cd clipflow
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app
```

Open <http://127.0.0.1:8000>.

If your Python installation does not include `venv`, install the `python3-venv`
package first, or install the dependencies for your user with
`python3 -m pip install --user -r requirements.txt`.

To use a specific yt-dlp or ffmpeg binary:

```bash
CLIPFLOW_YTDLP=/path/to/yt-dlp CLIPFLOW_FFMPEG=/path/to/ffmpeg uvicorn app:app
```

Downloaded files live in `clipflow/downloads/` by default. Override with `CLIPFLOW_DOWNLOAD_DIR=/some/path`.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/health` | Server, yt-dlp and ffmpeg availability |
| POST | `/api/metadata` | `{"url": "..."}` → real title, thumbnail, uploader, duration, formats with codec labels |
| POST | `/api/downloads` | `{"url": "...", "format_id": "..."}` → `{"job_id": "..."}` |
| GET | `/api/downloads/{job_id}` | status, percent, speed, eta, error |
| POST | `/api/downloads/{job_id}/cancel` | stop a queued/running job |
| GET | `/api/downloads/{job_id}/file` | stream the completed file |

Errors are structured JSON: `{"error": {"code": "...", "message": "..."}}`.

Each format includes the raw `vcodec`/`acodec` ids from yt-dlp and friendly
`video_codec`/`audio_codec` labels such as `H.264`, `VP9`, `AV1`, `AAC`, or
`Opus`. ClipFlow displays these labels but does not transcode.

## Tests

```bash
cd clipflow
pytest
```

The test suite mocks yt-dlp and DNS; it does not touch the network.

## Security notes

- Only `http`/`https` URLs on ports 80/443 are accepted.
- URLs with credentials, localhost, `.local`/`.internal` names, private, loopback, link-local, reserved and cloud metadata addresses are rejected.
- Hostnames are resolved before being handed to yt-dlp, and every resolved address must be globally routable.
- User input never reaches a shell: subprocesses use argument lists.
- Format ids are restricted to `A-Za-z0-9_.+-`.
- Output filenames are sanitized and the file endpoint only serves files inside the job's own directory.
- At most 3 jobs may be active at once. Further requests get HTTP 429.

## Known limitations

- Jobs and the metadata cache are in memory. Restarting the server loses job state; finished files stay on disk.
- There is no authentication or rate limiting. Do not expose this app directly to the public internet.
- DNS validation happens before yt-dlp runs. A malicious DNS server could rebind after validation, and yt-dlp follows its own redirects. Use an egress firewall if untrusted URLs are allowed.
- Private, members-only, age-restricted, and bot-checked YouTube videos generally need cookies/account access; ClipFlow does not collect cookies. It returns a short diagnostic instead.
- YouTube can change its extraction behavior at any time. Keep `yt-dlp` updated (`pip install -U yt-dlp`).
- No transcoding is offered. Audio-only and video-only formats are downloaded as provided; video-only formats are merged with best audio when ffmpeg exists.
- Progress is parsed from yt-dlp's per-stream output. For a video-only format that is merged with a separate audio stream, the bar can briefly restart when the audio download begins; the final size is reported once the merge finishes.
- Very long videos are allowed with a warning; there is no size or duration cap, so disk usage is the operator's responsibility.
- The job limit is per process. Running multiple uvicorn workers would multiply the limit and split in-memory state; run a single worker.

``````

### `clipflow/app.py`

``````text
"""ClipFlow - a small, single-server YouTube downloader built on yt-dlp.

The backend deliberately shells out to yt-dlp with an argument list. No user
input is ever interpolated into a shell string.
"""

from __future__ import annotations

import ipaddress
import json
import logging
import mimetypes
import os
import re
import secrets
import shutil
import signal
import socket
import subprocess
import threading
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel, Field
from starlette.exceptions import HTTPException as StarletteHTTPException

logger = logging.getLogger("clipflow")
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)

APP_DIR = Path(__file__).resolve().parent
INDEX_FILE = APP_DIR / "index.html"
DOWNLOAD_ROOT = Path(
    os.environ.get("CLIPFLOW_DOWNLOAD_DIR", APP_DIR / "downloads")
).resolve()
DOWNLOAD_ROOT.mkdir(parents=True, exist_ok=True)

MAX_URL_LENGTH = 2048
MAX_CONCURRENT_JOBS = 3
METADATA_TIMEOUT_SECONDS = 75
MAX_FORMATS = 120
METADATA_CACHE_TTL = 15 * 60
LONG_VIDEO_SECONDS = 2 * 60 * 60
FORMAT_ID_RE = re.compile(r"^[A-Za-z0-9_.+-]{1,100}$")

# Hostnames that must never be resolved/downloaded from. Private, loopback and
# link-local literal IPs are rejected separately with ipaddress.
BLOCKED_HOSTNAMES = {
    "localhost",
    "localhost.localdomain",
    "metadata",
    "metadata.google.internal",
    "metadata.google.com",
    "metadata.goog",
    "instance-data",
    "instance-data.ec2.internal",
}
BLOCKED_HOST_SUFFIXES = (
    ".localhost",
    ".local",
    ".internal",
    ".home.arpa",
    ".lan",
)

METADATA_CACHE: dict[str, tuple[float, dict[str, Any]]] = {}
CACHE_LOCK = threading.Lock()
HOST_CACHE: dict[tuple[str, str, int], tuple[float, tuple[str, ...]]] = {}
HOST_CACHE_TTL = 120
HOST_CACHE_LOCK = threading.Lock()


class ApiError(Exception):
    def __init__(self, status_code: int, code: str, message: str) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.message = message


class UrlPayload(BaseModel):
    url: str = Field(min_length=1, max_length=MAX_URL_LENGTH)


class DownloadPayload(UrlPayload):
    format_id: str = Field(min_length=1, max_length=100)


def _error_body(code: str, message: str) -> dict[str, Any]:
    return {"error": {"code": code, "message": message}}


def _ytdlp_path() -> str:
    configured = os.environ.get("CLIPFLOW_YTDLP")
    if configured:
        return configured
    return shutil.which("yt-dlp") or "yt-dlp"


def _ffmpeg_path() -> str | None:
    configured = os.environ.get("CLIPFLOW_FFMPEG")
    if configured:
        return configured if Path(configured).exists() else None
    return shutil.which("ffmpeg")


def _resolve_host(host: str, port: int) -> list[str]:
    """Resolve a hostname to unique IP strings. Kept separate for tests."""
    infos = socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)
    addresses: list[str] = []
    for info in infos:
        address = info[4][0]
        if address not in addresses:
            addresses.append(address)
    return addresses


def _resolve_with_timeout(host: str, port: int, timeout: float = 4.0) -> list[str] | None:
    result: list[list[str]] = []

    def worker() -> None:
        try:
            result.append(_resolve_host(host, port))
        except OSError:
            result.append([])

    thread = threading.Thread(target=worker, daemon=True)
    thread.start()
    thread.join(timeout)
    if thread.is_alive() or not result:
        return None
    return result[0]


def _ip_is_blocked(value: str) -> bool:
    try:
        address = ipaddress.ip_address(value)
    except ValueError:
        return True
    if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped:
        address = address.ipv4_mapped
    return (
        not address.is_global
        or address.is_private
        or address.is_loopback
        or address.is_link_local
        or address.is_multicast
        or address.is_reserved
        or address.is_unspecified
    )


def validate_media_url(raw: str) -> str:
    """Validate and normalize a user-supplied media URL.

    Rejects non-http(s) schemes, credentials, non-standard ports, local and
    private hosts, and hostnames that resolve to non-global addresses.
    """
    if not isinstance(raw, str):
        raise ApiError(400, "invalid_url", "The URL must be a string.")
    value = raw.strip()
    if not value:
        raise ApiError(400, "invalid_url", "Enter a video URL.")
    if len(value) > MAX_URL_LENGTH:
        raise ApiError(400, "invalid_url", "That URL is too long.")
    if any(ord(char) < 32 or ord(char) == 127 for char in value):
        raise ApiError(400, "invalid_url", "That URL contains invalid characters.")

    try:
        parsed = urlsplit(value)
    except ValueError:
        raise ApiError(400, "invalid_url", "Enter a complete http:// or https:// URL.")

    if parsed.scheme.lower() not in ("http", "https"):
        raise ApiError(400, "invalid_url", "Only http:// and https:// URLs are supported.")
    if parsed.username or parsed.password:
        raise ApiError(400, "invalid_url", "URLs with embedded credentials are not allowed.")
    if not parsed.hostname:
        raise ApiError(400, "invalid_url", "That URL is missing a hostname.")

    try:
        port = parsed.port
    except ValueError:
        raise ApiError(400, "invalid_url", "That URL has an invalid port.")
    if port is not None and port not in (80, 443):
        raise ApiError(400, "invalid_url", "Only ports 80 and 443 are supported.")

    try:
        host = parsed.hostname.rstrip(".").lower()
        host = host.encode("idna").decode("ascii")
    except (UnicodeError, ValueError):
        raise ApiError(400, "invalid_url", "That URL has an invalid hostname.")

    if host in BLOCKED_HOSTNAMES or host.endswith(BLOCKED_HOST_SUFFIXES):
        raise ApiError(400, "blocked_url", "Local and metadata addresses are not allowed.")

    # A literal IP can be checked immediately, without DNS.
    try:
        literal = ipaddress.ip_address(host)
    except ValueError:
        literal = None
    if literal is not None:
        if _ip_is_blocked(str(literal)):
            raise ApiError(400, "blocked_url", "Private, local and metadata addresses are not allowed.")
    else:
        resolved_port = port or (443 if parsed.scheme.lower() == "https" else 80)
        cache_key = (parsed.scheme.lower(), host, resolved_port)
        now = time.time()
        with HOST_CACHE_LOCK:
            cached = HOST_CACHE.get(cache_key)
            if cached is not None and now - cached[0] > HOST_CACHE_TTL:
                HOST_CACHE.pop(cache_key, None)
                cached = None
        if cached is not None:
            resolved = list(cached[1])
        else:
            resolved = _resolve_with_timeout(host, resolved_port)
            if not resolved:
                # The system resolver is occasionally slow; one retry avoids
                # turning a transient timeout into a failed download.
                resolved = _resolve_with_timeout(host, resolved_port)
            if not resolved:
                raise ApiError(400, "unresolvable_url", "We couldn't resolve that video URL.")
        if any(_ip_is_blocked(address) for address in resolved):
            with HOST_CACHE_LOCK:
                HOST_CACHE.pop(cache_key, None)
            raise ApiError(400, "blocked_url", "That URL points to a private or local address.")
        if cached is None:
            with HOST_CACHE_LOCK:
                if len(HOST_CACHE) > 256:
                    oldest = min(HOST_CACHE, key=lambda key: HOST_CACHE[key][0])
                    HOST_CACHE.pop(oldest, None)
                HOST_CACHE[cache_key] = (now, tuple(resolved))

    return value


def sanitize_filename(name: str, fallback: str = "clipflow-download") -> str:
    """Return a safe basename with no separators, traversal, or control chars."""
    cleaned = name.replace("\\", "/")
    cleaned = cleaned.split("/")[-1]
    cleaned = re.sub(r"[\x00-\x1f\x7f]", "", cleaned)
    cleaned = re.sub(r'[<>:"|?*]', "_", cleaned)
    cleaned = cleaned.replace("\r", "").replace("\n", "")
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" .")
    if cleaned in ("", ".", ".."):
        return fallback

    stem, extension = os.path.splitext(cleaned)
    if extension and not re.fullmatch(r"\.[A-Za-z0-9]{1,10}", extension):
        stem = cleaned
        extension = ""
    stem = stem[:170].rstrip(" .")
    if not stem or stem in (".", ".."):
        stem = fallback
    return f"{stem}{extension.lower()}"


def _is_inside(path: Path, root: Path) -> bool:
    try:
        path.resolve().relative_to(root.resolve())
        return True
    except ValueError:
        return False


def _trim_cache() -> None:
    now = time.time()
    with CACHE_LOCK:
        stale = [key for key, (stamp, _) in METADATA_CACHE.items() if now - stamp > METADATA_CACHE_TTL]
        for key in stale:
            METADATA_CACHE.pop(key, None)
        while len(METADATA_CACHE) > 64:
            oldest = min(METADATA_CACHE, key=lambda key: METADATA_CACHE[key][0])
            METADATA_CACHE.pop(oldest, None)


def _cache_metadata(url: str, metadata: dict[str, Any]) -> None:
    _trim_cache()
    with CACHE_LOCK:
        METADATA_CACHE[url] = (time.time(), metadata)


def _cached_format(url: str, format_id: str) -> dict[str, Any] | None:
    with CACHE_LOCK:
        entry = METADATA_CACHE.get(url)
    if not entry:
        return None
    if time.time() - entry[0] > METADATA_CACHE_TTL:
        return None
    for media_format in entry[1].get("formats", []):
        if media_format.get("id") == format_id:
            return media_format
    return None


def _safe_thumbnail(value: Any) -> str | None:
    if not isinstance(value, str):
        return None
    try:
        parsed = urlsplit(value)
    except ValueError:
        return None
    if parsed.scheme.lower() not in ("http", "https") or not parsed.hostname:
        return None
    host = parsed.hostname.rstrip(".").lower()
    if host in BLOCKED_HOSTNAMES or host.endswith(BLOCKED_HOST_SUFFIXES):
        return None
    try:
        literal = ipaddress.ip_address(host)
    except ValueError:
        literal = None
    if literal is not None and _ip_is_blocked(str(literal)):
        return None
    return value


def _number(value: Any) -> float | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return float(value)
    return None


VIDEO_CODEC_PREFIXES = (
    ("avc1", "H.264"),
    ("avc3", "H.264"),
    ("h264", "H.264"),
    ("hev1", "HEVC"),
    ("hvc1", "HEVC"),
    ("h265", "HEVC"),
    ("vp09", "VP9"),
    ("vp9", "VP9"),
    ("vp8", "VP8"),
    ("av01", "AV1"),
    ("av1", "AV1"),
    ("theora", "Theora"),
)
AUDIO_CODEC_PREFIXES = (
    ("mp4a", "AAC"),
    ("aac", "AAC"),
    ("opus", "Opus"),
    ("vorbis", "Vorbis"),
    ("mp3", "MP3"),
    ("ac-3", "Dolby AC-3"),
    ("ec-3", "Dolby E-AC-3"),
    ("flac", "FLAC"),
    ("alac", "ALAC"),
)


def _codec_label(value: Any, prefixes: tuple[tuple[str, str], ...]) -> str | None:
    """Turn a yt-dlp codec id into a short, readable label."""
    if not isinstance(value, str):
        return None
    codec = value.strip()
    if not codec or codec.lower() in {"none", "unknown", "n/a", "?"}:
        return None
    lowered = codec.lower()
    for prefix, label in prefixes:
        if lowered.startswith(prefix):
            return label
    return codec


def _extract_metadata(url: str) -> dict[str, Any]:
    ytdlp = _ytdlp_path()
    if not shutil.which(ytdlp) and not Path(ytdlp).is_file():
        raise ApiError(
            503,
            "yt_dlp_missing",
            "yt-dlp is not installed on the server. Install it and restart ClipFlow.",
        )

    command = [
        ytdlp,
        "--dump-single-json",
        "--no-playlist",
        "--no-warnings",
        "--no-progress",
        "--socket-timeout",
        "20",
        "--retries",
        "2",
        url,
    ]
    try:
        completed = subprocess.run(
            command,
            capture_output=True,
            text=True,
            timeout=METADATA_TIMEOUT_SECONDS,
            check=False,
            env={**os.environ, "PYTHONUNBUFFERED": "1"},
        )
    except subprocess.TimeoutExpired:
        raise ApiError(
            504,
            "metadata_timeout",
            "yt-dlp took too long to read this video. Try again or choose a shorter video.",
        )
    except OSError as exc:
        logger.exception("Could not start yt-dlp: %s", exc)
        raise ApiError(503, "yt_dlp_missing", "yt-dlp could not be started on the server.")

    if completed.returncode != 0:
        logger.warning("yt-dlp metadata failed (%s): %s", completed.returncode, completed.stderr[-2000:])
        code, message = _friendly_ytdlp_error(completed.stderr)
        status_code = {
            "private_video": 403,
            "age_restricted": 403,
            "access_denied": 403,
            "unavailable_video": 404,
            "bot_check": 502,
            "network_error": 502,
            "yt_dlp_failure": 502,
        }.get(code, 422)
        raise ApiError(status_code, code, message)

    try:
        info = json.loads(completed.stdout)
    except (json.JSONDecodeError, TypeError):
        logger.error("yt-dlp returned unparseable metadata")
        raise ApiError(502, "yt_dlp_failure", "yt-dlp returned an unexpected response. Try again.")

    if not isinstance(info, dict):
        raise ApiError(502, "yt_dlp_failure", "yt-dlp returned an unexpected response. Try again.")
    if info.get("_type") == "playlist":
        entries = info.get("entries") or []
        if not entries:
            raise ApiError(422, "unavailable_video", "That playlist has no downloadable videos.")
        info = entries[0]

    formats: list[dict[str, Any]] = []
    raw_formats = info.get("formats")
    if isinstance(raw_formats, list):
        for item in raw_formats:
            if not isinstance(item, dict):
                continue
            format_id = item.get("format_id")
            if not isinstance(format_id, str) or not format_id:
                continue
            if item.get("protocol") == "mhtml":
                continue
            vcodec = item.get("vcodec")
            acodec = item.get("acodec")
            has_video = bool(vcodec and vcodec != "none")
            has_audio = bool(acodec and acodec != "none")
            if not has_video and not has_audio:
                continue
            height = _number(item.get("height"))
            resolution = item.get("resolution")
            if not isinstance(resolution, str) or not resolution:
                if height:
                    resolution = f"{int(height)}p"
                elif has_video:
                    resolution = "Video"
                else:
                    resolution = "Audio"
            filesize = _number(item.get("filesize")) or _number(item.get("filesize_approx"))
            abr = _number(item.get("abr"))
            ext = item.get("ext")
            formats.append(
                {
                    "id": format_id,
                    "ext": ext if isinstance(ext, str) else "?",
                    "resolution": resolution,
                    "filesize": int(filesize) if filesize else None,
                    "has_video": has_video,
                    "has_audio": has_audio,
                    "requires_merge": has_video and not has_audio,
                    "vcodec": vcodec if has_video and isinstance(vcodec, str) else None,
                    "acodec": acodec if has_audio and isinstance(acodec, str) else None,
                    "video_codec": _codec_label(vcodec, VIDEO_CODEC_PREFIXES) if has_video else None,
                    "audio_codec": _codec_label(acodec, AUDIO_CODEC_PREFIXES) if has_audio else None,
                    "filesize_estimate": filesize is not None
                    and item.get("filesize") is None,
                    "height": int(height) if height else None,
                    "fps": int(_number(item.get("fps")) or 0) or None,
                    "abr": round(abr, 1) if abr else None,
                    "format_note": item.get("format_note")
                    if isinstance(item.get("format_note"), str)
                    else None,
                }
            )
            if len(formats) >= MAX_FORMATS:
                break

    if not formats:
        raise ApiError(
            422,
            "no_formats",
            "No downloadable formats were found for this video.",
        )

    duration = _number(info.get("duration"))
    title = info.get("title")
    if not isinstance(title, str) or not title.strip():
        title = "Untitled video"
    uploader = info.get("uploader") or info.get("channel") or info.get("creator")
    if not isinstance(uploader, str) or not uploader.strip():
        uploader = "Unknown uploader"

    metadata = {
        "url": url,
        "title": title.strip(),
        "thumbnail": _safe_thumbnail(info.get("thumbnail")),
        "uploader": uploader.strip(),
        "duration": int(duration) if duration is not None else None,
        "is_long": bool(duration and duration > LONG_VIDEO_SECONDS),
        "webpage_url": info.get("webpage_url") if isinstance(info.get("webpage_url"), str) else url,
        "formats": formats,
    }
    _cache_metadata(url, metadata)
    return metadata


def _friendly_ytdlp_error(stderr: str) -> tuple[str, str]:
    text = (stderr or "").lower()
    if any(token in text for token in ("confirm you're not a bot", "confirm you\u2019re not a bot", "not a bot")):
        return (
            "bot_check",
            "YouTube blocked this request with a bot check. Try again later or use a different video.",
        )
    if any(
        token in text
        for token in ("age-restricted", "age restricted", "confirm your age", "age verification")
    ):
        return (
            "age_restricted",
            "This video is age-restricted. yt-dlp would need account cookies, which ClipFlow doesn't collect.",
        )
    if any(
        token in text
        for token in ("private video", "video is private", "login required", "members-only", "members only")
    ):
        return (
            "private_video",
            "This video is private or requires a signed-in account, so it can't be downloaded.",
        )
    if "unsupported url" in text:
        return "unsupported_url", "yt-dlp doesn't support this URL. Use a direct video page or a supported media URL."
    if "requested format is not available" in text or "format is not available" in text:
        return "format_unavailable", "That format is no longer available. Fetch the formats again and choose another."
    if any(token in text for token in ("video unavailable", "video is unavailable", "has been removed", "does not exist", "not available", "unavailable")):
        return "unavailable_video", "This video is unavailable, deleted, or blocked in this region."
    if "ffmpeg" in text and any(token in text for token in ("not found", "not installed", "install")):
        return (
            "ffmpeg_missing",
            "ffmpeg is required for this format, but it is not installed on the server.",
        )
    if any(token in text for token in ("timed out", "timeout", "temporary failure", "network is unreachable")):
        return "network_error", "The server couldn't reach the video host. Check the connection and try again."
    if "http error 403" in text or "forbidden" in text:
        return "access_denied", "The video host refused the request. The media may be region-locked or expired."
    return "yt_dlp_failure", "yt-dlp could not process this video. Try another URL or format."


@dataclass
class Job:
    id: str
    url: str
    format_id: str
    status: str = "queued"
    percent: float | None = None
    speed: float | None = None
    eta: int | None = None
    downloaded_bytes: int | None = None
    total_bytes: int | None = None
    error: str | None = None
    error_code: str | None = None
    file_path: str | None = None
    filename: str | None = None
    requires_merge: bool = False
    created_at: float = field(default_factory=time.time)
    started_at: float | None = None
    finished_at: float | None = None
    cancel_event: threading.Event = field(default_factory=threading.Event)
    process: subprocess.Popen[str] | None = None

    def public(self) -> dict[str, Any]:
        return {
            "job_id": self.id,
            "status": self.status,
            "percent": round(self.percent, 2) if self.percent is not None else None,
            "speed": round(self.speed, 2) if self.speed is not None else None,
            "eta": self.eta,
            "downloaded_bytes": self.downloaded_bytes,
            "total_bytes": self.total_bytes,
            "error": self.error,
            "error_code": self.error_code,
            "filename": self.filename,
            "download_url": f"/api/downloads/{self.id}/file" if self.status == "completed" else None,
        }


class DownloadManager:
    def __init__(self) -> None:
        self.jobs: dict[str, Job] = {}
        self.lock = threading.RLock()

    def reset(self) -> None:
        with self.lock:
            for job in self.jobs.values():
                job.cancel_event.set()
                process = job.process
                if process is not None and process.poll() is None:
                    _terminate_process(process)
            self.jobs.clear()

    def active_count(self) -> int:
        with self.lock:
            return sum(1 for job in self.jobs.values() if job.status in ("queued", "downloading", "processing"))

    def create(self, url: str, format_id: str) -> Job:
        with self.lock:
            if self.active_count() >= MAX_CONCURRENT_JOBS:
                raise ApiError(
                    429,
                    "too_many_jobs",
                    f"ClipFlow is already handling {MAX_CONCURRENT_JOBS} downloads. Wait for one to finish or cancel one.",
                )
            cached = _cached_format(url, format_id)
            if cached is None:
                with CACHE_LOCK:
                    known_url = url in METADATA_CACHE and time.time() - METADATA_CACHE[url][0] <= METADATA_CACHE_TTL
                if known_url:
                    raise ApiError(
                        400,
                        "unknown_format",
                        "That format isn't part of this video. Fetch the formats again and pick one.",
                    )
            requires_merge = bool(cached and cached.get("requires_merge"))
            if requires_merge and _ffmpeg_path() is None:
                raise ApiError(
                    422,
                    "ffmpeg_missing",
                    "This format has no audio track and requires ffmpeg to merge it with the best audio. "
                    "Install ffmpeg on the server or choose a format that already includes audio.",
                )
            job = Job(
                id=secrets.token_hex(16),
                url=url,
                format_id=format_id,
                requires_merge=requires_merge,
            )
            job_dir = DOWNLOAD_ROOT / job.id
            job_dir.mkdir(parents=True, exist_ok=True)
            self.jobs[job.id] = job
        return job

    def get(self, job_id: str) -> Job:
        with self.lock:
            job = self.jobs.get(job_id)
        if job is None:
            raise ApiError(404, "job_not_found", "That download job doesn't exist.")
        return job

    def cancel(self, job_id: str) -> Job:
        job = self.get(job_id)
        with self.lock:
            if job.status in ("completed", "failed"):
                raise ApiError(409, "not_cancellable", "This download has already finished.")
            job.cancel_event.set()
            if job.status == "cancelled":
                return job
            job.status = "cancelled"
            job.finished_at = time.time()
            job.speed = None
            job.eta = None
            process = job.process
            was_queued = job.started_at is None
        if process is not None and process.poll() is None:
            _terminate_process(process)
        if was_queued:
            _remove_job_dir(job.id)
        return job

    def set_status(self, job: Job, status: str) -> None:
        with self.lock:
            if job.status == "cancelled":
                return
            job.status = status
            if status == "downloading" and job.started_at is None:
                job.started_at = time.time()

    def set_progress(
        self,
        job: Job,
        *,
        downloaded: int | None,
        total: int | None,
        speed: float | None,
        eta: int | None,
        percent: float | None,
    ) -> None:
        with self.lock:
            if job.status == "cancelled":
                return
            job.downloaded_bytes = downloaded
            if total is not None:
                job.total_bytes = total
            job.speed = speed
            job.eta = eta
            job.percent = percent

    def fail(self, job: Job, code: str, message: str) -> None:
        with self.lock:
            if job.status == "cancelled":
                return
            job.status = "failed"
            job.error_code = code
            job.error = message
            job.speed = None
            job.eta = None
            job.finished_at = time.time()
            job.process = None
        _remove_job_dir(job.id)

    def complete(self, job: Job, file_path: Path) -> None:
        with self.lock:
            if job.status == "cancelled":
                _remove_job_dir(job.id)
                return
            job.status = "completed"
            job.percent = 100.0
            job.speed = None
            job.eta = 0
            job.finished_at = time.time()
            job.file_path = str(file_path)
            job.filename = sanitize_filename(file_path.name)
            job.process = None
            try:
                actual_size = file_path.stat().st_size
                job.total_bytes = actual_size
                job.downloaded_bytes = actual_size
            except OSError as exc:
                logger.warning("Could not stat finished file %s: %s", file_path, exc)

    def finish_cancelled(self, job: Job) -> None:
        with self.lock:
            job.status = "cancelled"
            job.finished_at = job.finished_at or time.time()
            job.speed = None
            job.eta = None
            job.process = None
        _remove_job_dir(job.id)


manager = DownloadManager()
app = FastAPI(title="ClipFlow", version="1.0.0")


@app.exception_handler(ApiError)
async def handle_api_error(_request: Request, exc: ApiError) -> JSONResponse:
    return JSONResponse(status_code=exc.status_code, content=_error_body(exc.code, exc.message))


@app.exception_handler(RequestValidationError)
async def handle_validation_error(_request: Request, exc: RequestValidationError) -> JSONResponse:
    fields: list[str] = []
    for error in exc.errors():
        location = [str(part) for part in error.get("loc", ()) if part != "body"]
        if location:
            fields.append(location[0])
        if error.get("type") == "json_invalid":
            fields = []
            break
    if fields:
        message = "The request body is invalid. Check: " + ", ".join(sorted(set(fields))) + "."
    else:
        message = "The request body is invalid. Send JSON with a 'url' field."
    return JSONResponse(status_code=400, content=_error_body("invalid_request", message))


@app.exception_handler(StarletteHTTPException)
async def handle_http_error(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    if request.url.path.startswith("/api/"):
        message = exc.detail if isinstance(exc.detail, str) else "The request could not be completed."
        return JSONResponse(
            status_code=exc.status_code,
            content=_error_body("http_error", message),
        )
    return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})


@app.exception_handler(Exception)
async def handle_unexpected_error(_request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled error: %s", exc)
    return JSONResponse(
        status_code=500,
        content=_error_body("internal_error", "Something went wrong on the server. Try again."),
    )


def _terminate_process(process: subprocess.Popen[str]) -> None:
    if process.poll() is not None:
        return
    try:
        os.killpg(os.getpgid(process.pid), signal.SIGTERM)
    except (ProcessLookupError, PermissionError, OSError):
        try:
            process.terminate()
        except OSError:
            return
    try:
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(os.getpgid(process.pid), signal.SIGKILL)
        except (ProcessLookupError, PermissionError, OSError):
            try:
                process.kill()
            except OSError:
                logger.warning("Could not kill yt-dlp process %s", process.pid)


def _remove_job_dir(job_id: str) -> None:
    job_dir = (DOWNLOAD_ROOT / job_id).resolve()
    if not _is_inside(job_dir, DOWNLOAD_ROOT):
        return
    shutil.rmtree(job_dir, ignore_errors=True)


def _parse_progress_line(line: str) -> tuple[int | None, int | None, float | None, int | None] | None:
    if not line.startswith("CFPROGRESS|"):
        return None
    parts = line.strip().split("|")
    if len(parts) < 6:
        return None

    def number(value: str) -> float | None:
        value = value.strip()
        if value in ("", "NA", "None", "N/A"):
            return None
        try:
            parsed = float(value)
        except ValueError:
            return None
        return parsed if parsed >= 0 else None

    downloaded = number(parts[1])
    total = number(parts[2])
    estimate = number(parts[3])
    speed = number(parts[4])
    eta = number(parts[5])
    total_value = total or estimate
    return (
        int(downloaded) if downloaded is not None else None,
        int(total_value) if total_value is not None else None,
        speed,
        int(eta) if eta is not None else None,
    )


def _find_finished_file(job_dir: Path) -> Path | None:
    if not job_dir.is_dir():
        return None
    candidates: list[Path] = []
    for path in job_dir.iterdir():
        if not path.is_file():
            continue
        if path.name.endswith((".part", ".ytdl", ".temp", ".json", ".jpg", ".jpeg", ".png", ".webp")):
            continue
        try:
            if path.stat().st_size <= 0:
                continue
        except OSError:
            continue
        candidates.append(path)
    if not candidates:
        return None
    finals = [path for path in candidates if not re.search(r"\.f\d+\.[A-Za-z0-9]+$", path.name)]
    pool = finals or candidates
    return max(pool, key=lambda path: (path.stat().st_mtime, path.stat().st_size))


def _download_command(job: Job, job_dir: Path) -> list[str]:
    selector = f"{job.format_id}+bestaudio" if job.requires_merge else job.format_id
    output_template = str(job_dir / "%(title).120B [%(id)s].%(ext)s")
    return [
        _ytdlp_path(),
        "--no-playlist",
        "--newline",
        "--no-colors",
        "--no-warnings",
        "--progress",
        "--progress-template",
        "download:CFPROGRESS|%(progress.downloaded_bytes)s|%(progress.total_bytes)s|"
        "%(progress.total_bytes_estimate)s|%(progress.speed)s|%(progress.eta)s",
        "--socket-timeout",
        "20",
        "--retries",
        "3",
        "--restrict-filenames",
        "--no-mtime",
        "-f",
        selector,
        "-o",
        output_template,
        job.url,
    ]


def _run_job(job: Job) -> None:
    job_dir = DOWNLOAD_ROOT / job.id
    try:
        if job.cancel_event.is_set():
            manager.finish_cancelled(job)
            return
        if not shutil.which(_ytdlp_path()) and not Path(_ytdlp_path()).is_file():
            manager.fail(
                job,
                "yt_dlp_missing",
                "yt-dlp is not installed on the server. Install it and restart ClipFlow.",
            )
            return
        if job.requires_merge and _ffmpeg_path() is None:
            manager.fail(
                job,
                "ffmpeg_missing",
                "This format has no audio track and requires ffmpeg to merge it with the best audio. "
                "Install ffmpeg on the server or choose a format that already includes audio.",
            )
            return

        job_dir.mkdir(parents=True, exist_ok=True)
        manager.set_status(job, "downloading")
        command = _download_command(job, job_dir)
        try:
            process = subprocess.Popen(
                command,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                errors="replace",
                bufsize=1,
                start_new_session=True,
                env={**os.environ, "PYTHONUNBUFFERED": "1"},
            )
        except OSError as exc:
            logger.exception("Could not launch yt-dlp: %s", exc)
            manager.fail(job, "yt_dlp_missing", "yt-dlp could not be started on the server.")
            return

        with manager.lock:
            job.process = process
            cancelled_before_read = job.cancel_event.is_set()

        if cancelled_before_read:
            _terminate_process(process)
            manager.finish_cancelled(job)
            return

        recent_lines: list[str] = []
        assert process.stdout is not None
        for raw_line in process.stdout:
            line = raw_line.rstrip("\n")
            if not line:
                continue
            recent_lines.append(line)
            if len(recent_lines) > 60:
                recent_lines.pop(0)
            parsed = _parse_progress_line(line)
            if parsed is not None:
                downloaded, total, speed, eta = parsed
                percent = None
                if total and downloaded is not None:
                    percent = min(100.0, max(0.0, downloaded / total * 100.0))
                manager.set_progress(
                    job,
                    downloaded=downloaded,
                    total=total,
                    speed=speed,
                    eta=eta,
                    percent=percent,
                )
            elif "[Merger]" in line or "[VideoConvertor]" in line or "[FixupM3u8]" in line:
                manager.set_status(job, "processing")
            if job.cancel_event.is_set():
                _terminate_process(process)
                break

        try:
            return_code = process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            _terminate_process(process)
            return_code = process.wait(timeout=5)

        with manager.lock:
            job.process = None

        if job.cancel_event.is_set():
            manager.finish_cancelled(job)
            return
        if return_code != 0:
            logger.warning("yt-dlp download failed (%s): %s", return_code, "\n".join(recent_lines[-20:]))
            code, message = _friendly_ytdlp_error("\n".join(recent_lines))
            manager.fail(job, code, message)
            return

        finished = _find_finished_file(job_dir)
        if finished is None:
            manager.fail(
                job,
                "missing_output",
                "yt-dlp finished but produced no file. The selected format may not be downloadable.",
            )
            return
        manager.complete(job, finished)
    except Exception as exc:  # Keep worker failures user-facing, never a traceback.
        logger.exception("Download worker crashed: %s", exc)
        manager.fail(job, "internal_error", "The download failed unexpectedly. Try again.")


def _start_worker(job: Job) -> None:
    thread = threading.Thread(target=_run_job, args=(job,), name=f"clipflow-{job.id[:8]}", daemon=True)
    thread.start()


@app.get("/api/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "service": "clipflow",
        "yt_dlp_available": bool(shutil.which(_ytdlp_path()) or Path(_ytdlp_path()).is_file()),
        "ffmpeg_available": _ffmpeg_path() is not None,
        "active_jobs": manager.active_count(),
        "max_active_jobs": MAX_CONCURRENT_JOBS,
    }


@app.post("/api/metadata")
def metadata(payload: UrlPayload) -> dict[str, Any]:
    url = validate_media_url(payload.url)
    return _extract_metadata(url)


@app.post("/api/downloads", status_code=202)
def create_download(payload: DownloadPayload) -> dict[str, Any]:
    url = validate_media_url(payload.url)
    if not FORMAT_ID_RE.fullmatch(payload.format_id):
        raise ApiError(
            400,
            "invalid_format",
            "The format id is invalid. Fetch the formats again and choose one.",
        )
    job = manager.create(url, payload.format_id)
    _start_worker(job)
    return {"job_id": job.id, "status": job.status}


@app.get("/api/downloads/{job_id}")
def download_status(job_id: str) -> dict[str, Any]:
    return manager.get(job_id).public()


@app.post("/api/downloads/{job_id}/cancel")
def cancel_download(job_id: str) -> dict[str, Any]:
    return manager.cancel(job_id).public()


@app.get("/api/downloads/{job_id}/file")
def download_file(job_id: str) -> FileResponse:
    job = manager.get(job_id)
    if job.status != "completed":
        raise ApiError(409, "not_ready", "This download is not ready yet.")
    if not job.file_path:
        raise ApiError(404, "file_not_found", "The download file is no longer available.")
    path = Path(job.file_path).resolve()
    if not _is_inside(path, DOWNLOAD_ROOT / job.id) or not _is_inside(path, DOWNLOAD_ROOT):
        raise ApiError(404, "file_not_found", "The download file is no longer available.")
    if not path.is_file():
        raise ApiError(404, "file_not_found", "The download file is no longer available.")
    media_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    return FileResponse(
        path,
        media_type=media_type,
        filename=sanitize_filename(path.name),
        headers={"Cache-Control": "private, no-store"},
    )


@app.get("/", include_in_schema=False)
def index() -> FileResponse:
    if not INDEX_FILE.is_file():
        raise ApiError(500, "frontend_missing", "index.html is missing from the app directory.")
    return FileResponse(INDEX_FILE, media_type="text/html")

``````

### `clipflow/index.html`

``````text
<!DOCTYPE html>
<html lang="en" class="h-full scroll-smooth">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="dark">
  <meta name="description" content="ClipFlow downloads YouTube media with yt-dlp. Pick a real format, watch real progress.">
  <title>ClipFlow — YouTube media, cleanly</title>
  <link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'%3E%3Crect width='64' height='64' rx='16' fill='%238b9cff'/%3E%3Cpath d='M26 20l18 12-18 12z' fill='%230b0d12'/%3E%3C/svg%3E">
  <script src="https://cdn.tailwindcss.com"></script>
  <script>
    tailwind.config = {
      theme: {
        extend: {
          colors: {
            ink: {
              950: '#08090c',
              900: '#0b0d12',
              850: '#10131a',
              800: '#151923',
              700: '#1d2230',
              600: '#2a3042',
            },
            accent: {
              DEFAULT: '#8b9cff',
              soft: '#b3beff',
              dim: '#5f6fd6',
            },
          },
          fontFamily: {
            sans: ['ui-sans-serif', 'system-ui', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', '"Helvetica Neue"', 'Arial', 'sans-serif'],
            mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', '"Liberation Mono"', 'monospace'],
          },
          boxShadow: {
            card: '0 24px 70px -34px rgba(0, 0, 0, 0.9)',
            glow: '0 0 0 1px rgba(139, 156, 255, 0.18), 0 18px 60px -24px rgba(139, 156, 255, 0.3)',
          },
        },
      },
    };
  </script>
  <style>
    html { background: #08090c; }
    body { font-feature-settings: "ss01" 1, "cv11" 1; }
    ::selection { background: rgba(139, 156, 255, 0.3); color: #fff; }
    .skeleton {
      background: linear-gradient(100deg, #151923 20%, #1d2230 42%, #151923 64%);
      background-size: 220% 100%;
      animation: shimmer 1.5s ease-in-out infinite;
    }
    @keyframes shimmer {
      0% { background-position: 120% 0; }
      100% { background-position: -120% 0; }
    }
    .indeterminate::after {
      content: "";
      position: absolute;
      inset: 0 auto 0 0;
      width: 38%;
      border-radius: 999px;
      background: linear-gradient(90deg, transparent, #8b9cff, transparent);
      animation: sweep 1.25s ease-in-out infinite;
    }
    @keyframes sweep {
      0% { transform: translateX(-110%); }
      100% { transform: translateX(310%); }
    }
    .format-list { scrollbar-width: thin; scrollbar-color: #2a3042 transparent; }
    .format-list::-webkit-scrollbar { width: 8px; }
    .format-list::-webkit-scrollbar-thumb { background: #2a3042; border-radius: 999px; }
    .format-list::-webkit-scrollbar-track { background: transparent; }
    .format-option input:checked + .format-card { border-color: rgba(139, 156, 255, 0.6); background: rgba(139, 156, 255, 0.07); }
    .format-option input:checked + .format-card .format-dot { border-color: #8b9cff; }
    .format-option input:checked + .format-card .format-dot > span { opacity: 1; }
    [hidden] { display: none !important; }
    @media (prefers-reduced-motion: reduce) {
      *, *::before, *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
        scroll-behavior: auto !important;
      }
    }
  </style>
</head>
<body class="min-h-full bg-ink-950 font-sans text-zinc-100 antialiased">
  <div aria-hidden="true" class="pointer-events-none fixed inset-x-0 top-0 -z-0 h-[32rem] overflow-hidden">
    <div class="absolute left-1/2 top-[-18rem] h-[34rem] w-[54rem] -translate-x-1/2 rounded-full bg-accent/10 blur-[120px]"></div>
  </div>

  <div class="relative z-10 flex min-h-screen flex-col">
    <header class="border-b border-white/5">
      <div class="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-5 py-5 sm:px-8">
        <a href="/" class="group flex items-center gap-3 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-4 focus-visible:ring-offset-ink-950">
          <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-accent text-ink-950 shadow-glow transition-transform duration-200 group-hover:scale-[1.03]">
            <svg viewBox="0 0 24 24" class="h-5 w-5" fill="currentColor" aria-hidden="true">
              <path d="M8.5 6.2c0-.8.9-1.3 1.6-.9l8.1 5.1c.7.4.7 1.4 0 1.8l-8.1 5.1c-.7.4-1.6-.1-1.6-.9V6.2Z"/>
              <path d="M5 7.5a1 1 0 1 0-2 0v9a1 1 0 1 0 2 0v-9Z" opacity=".55"/>
            </svg>
          </span>
          <span>
            <span class="block text-[15px] font-semibold tracking-tight text-white">ClipFlow</span>
            <span class="block text-xs text-zinc-400">YouTube media, cleanly.</span>
          </span>
        </a>

        <div id="server-chip" class="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.03] px-3 py-1.5 text-xs font-medium text-zinc-400" role="status">
          <span id="server-dot" class="h-1.5 w-1.5 rounded-full bg-zinc-500"></span>
          <span id="server-text">Checking server</span>
        </div>
      </div>
    </header>

    <main class="mx-auto w-full max-w-6xl flex-1 px-5 pb-20 pt-12 sm:px-8 sm:pt-16">
      <section class="mx-auto max-w-3xl text-center">
        <p class="mx-auto mb-5 inline-flex items-center gap-2 rounded-full border border-accent/20 bg-accent/[0.07] px-3.5 py-1.5 text-xs font-medium tracking-wide text-accent-soft">
          <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
          Real formats. Real progress. No timers.
        </p>
        <h1 class="text-balance text-4xl font-semibold leading-[1.08] tracking-[-0.035em] text-white sm:text-5xl md:text-6xl">
          Bring the clip.<br class="hidden sm:block">
          <span class="text-zinc-500">Skip the clutter.</span>
        </h1>
        <p class="mx-auto mt-5 max-w-xl text-pretty text-[15px] leading-7 text-zinc-400 sm:text-base">
          Paste a video link, inspect the formats yt-dlp actually found, then download it. Progress comes straight from the downloader.
        </p>
      </section>

      <section class="mx-auto mt-10 max-w-3xl" aria-labelledby="url-heading">
        <div class="rounded-2xl border border-white/[0.07] bg-ink-900/90 p-2 shadow-card backdrop-blur">
          <form id="url-form" novalidate>
            <label for="url-input" id="url-heading" class="sr-only">Video URL</label>
            <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
              <div class="relative min-w-0 flex-1">
                <svg class="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-zinc-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                  <path d="M10.6 13.4a4 4 0 0 0 5.7 0l2.4-2.4a4 4 0 1 0-5.7-5.7L11.6 6.7" stroke-linecap="round"/>
                  <path d="M13.4 10.6a4 4 0 0 0-5.7 0l-2.4 2.4a4 4 0 1 0 5.7 5.7l1.4-1.4" stroke-linecap="round"/>
                </svg>
                <input
                  id="url-input"
                  name="url"
                  type="url"
                  inputmode="url"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="https://www.youtube.com/watch?v=…"
                  class="h-14 w-full rounded-xl border border-transparent bg-ink-850 pl-12 pr-4 text-[15px] text-white placeholder:text-zinc-500 transition-colors duration-200 hover:border-white/10 focus:border-accent/50 focus:bg-ink-800 focus:outline-none focus:ring-4 focus:ring-accent/10"
                  aria-describedby="url-help url-error"
                  aria-invalid="false"
                >
              </div>
              <button
                id="fetch-btn"
                type="submit"
                class="inline-flex h-14 shrink-0 items-center justify-center gap-2 rounded-xl bg-accent px-6 text-sm font-semibold text-ink-950 transition duration-200 hover:bg-accent-soft focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-ink-950 disabled:cursor-not-allowed disabled:opacity-60 sm:min-w-[10.5rem]"
              >
                <span id="fetch-spinner" hidden class="h-4 w-4 animate-spin rounded-full border-2 border-ink-950/30 border-t-ink-950" aria-hidden="true"></span>
                <span id="fetch-label">Fetch formats</span>
              </button>
            </div>
            <div class="flex flex-col gap-1 px-2 pb-1 pt-2 sm:flex-row sm:items-center sm:justify-between">
              <p id="url-help" class="text-xs text-zinc-400">Paste a YouTube link. Enter submits. Only download media you have rights to use.</p>
              <p id="url-error" class="text-xs font-medium text-rose-300" role="alert" hidden></p>
            </div>
          </form>
        </div>

        <div id="server-banner" hidden class="mt-4 flex gap-3 rounded-xl border border-amber-400/20 bg-amber-400/[0.07] px-4 py-3 text-sm text-amber-100">
          <svg class="mt-0.5 h-4 w-4 shrink-0 text-amber-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
            <path d="M12 8v5m0 3.5v.5M10.3 3.9 2.6 17.4A1.8 1.8 0 0 0 4.2 20h15.6a1.8 1.8 0 0 0 1.6-2.6L13.7 3.9a1.8 1.8 0 0 0-3.4 0Z" stroke-linecap="round"/>
          </svg>
          <p id="server-banner-text" class="leading-6"></p>
        </div>
      </section>

      <section class="mx-auto mt-8 max-w-5xl" aria-live="polite" aria-busy="false" id="workspace">
        <div id="empty-state" class="rounded-2xl border border-dashed border-white/10 bg-white/[0.015] px-6 py-14 text-center">
          <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-ink-850 text-zinc-500">
            <svg viewBox="0 0 24 24" class="h-6 w-6" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <rect x="3" y="5" width="18" height="14" rx="3"/>
              <path d="m10 9 5 3-5 3V9Z" stroke-linejoin="round"/>
            </svg>
          </div>
          <h2 class="mt-5 text-base font-medium text-zinc-200">No video loaded yet</h2>
          <p class="mx-auto mt-2 max-w-md text-sm leading-6 text-zinc-400">Drop a link above to see the title, uploader, duration, and every downloadable format yt-dlp reports.</p>
        </div>

        <div id="loading-state" hidden aria-label="Loading video metadata">
          <div class="grid gap-5 lg:grid-cols-[1.05fr_1fr]">
            <div class="overflow-hidden rounded-2xl border border-white/[0.07] bg-ink-900">
              <div class="skeleton aspect-video w-full"></div>
              <div class="space-y-3 p-5">
                <div class="skeleton h-3 w-24 rounded"></div>
                <div class="skeleton h-5 w-4/5 rounded"></div>
                <div class="skeleton h-3 w-36 rounded"></div>
              </div>
            </div>
            <div class="rounded-2xl border border-white/[0.07] bg-ink-900 p-5">
              <div class="skeleton h-3 w-28 rounded"></div>
              <div class="mt-5 space-y-3">
                <div class="skeleton h-16 w-full rounded-xl"></div>
                <div class="skeleton h-16 w-full rounded-xl"></div>
                <div class="skeleton h-16 w-full rounded-xl"></div>
                <div class="skeleton h-16 w-full rounded-xl"></div>
              </div>
              <div class="skeleton mt-5 h-12 w-full rounded-xl"></div>
            </div>
          </div>
        </div>

        <div id="metadata-error" hidden class="rounded-2xl border border-rose-400/20 bg-rose-400/[0.06] p-6 sm:p-7">
          <div class="flex items-start gap-4">
            <span class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-rose-400/10 text-rose-300">
              <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path d="M12 8v5m0 3.5v.5" stroke-linecap="round"/>
                <circle cx="12" cy="12" r="9"/>
              </svg>
            </span>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-base font-semibold text-rose-50">Couldn’t load that video</h2>
                <span id="metadata-error-code" class="rounded-full border border-rose-300/20 px-2 py-0.5 font-mono text-[10px] uppercase tracking-wider text-rose-300"></span>
              </div>
              <p id="metadata-error-message" class="mt-2 text-sm leading-6 text-rose-100/80"></p>
              <p class="mt-3 text-xs text-rose-200/80">Fix the link above and submit again. Private, deleted, and age-restricted videos cannot be fetched.</p>
            </div>
          </div>
        </div>

        <div id="result" hidden class="grid items-start gap-5 lg:grid-cols-[1.05fr_1fr]">
          <article class="overflow-hidden rounded-2xl border border-white/[0.07] bg-ink-900 shadow-card">
            <div class="relative aspect-video w-full overflow-hidden bg-ink-850">
              <img id="thumbnail" alt="" class="h-full w-full object-cover" loading="lazy" referrerpolicy="no-referrer">
              <div id="thumb-fallback" hidden class="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-ink-800 to-ink-950 text-zinc-600">
                <svg viewBox="0 0 24 24" class="h-10 w-10" fill="none" stroke="currentColor" stroke-width="1.3" aria-hidden="true">
                  <rect x="3" y="5" width="18" height="14" rx="3"/>
                  <path d="m10 9 5 3-5 3V9Z" stroke-linejoin="round"/>
                </svg>
              </div>
              <span id="duration-badge" class="absolute bottom-3 right-3 rounded-md bg-black/80 px-2 py-1 font-mono text-[11px] font-medium tracking-wide text-white backdrop-blur"></span>
            </div>
            <div class="p-5 sm:p-6">
              <p id="uploader" class="text-xs font-medium uppercase tracking-[0.14em] text-accent-soft/80"></p>
              <h2 id="video-title" class="mt-2 text-pretty text-xl font-semibold leading-snug tracking-tight text-white sm:text-2xl"></h2>
              <div class="mt-4 flex flex-wrap items-center gap-2 text-xs text-zinc-400">
                <span id="source-label" class="rounded-md border border-white/10 bg-white/[0.03] px-2 py-1">yt-dlp source</span>
                <span id="format-count" class="rounded-md border border-white/10 bg-white/[0.03] px-2 py-1"></span>
              </div>
              <p id="long-warning" hidden class="mt-4 flex gap-2 rounded-xl border border-amber-400/15 bg-amber-400/[0.05] px-3 py-2.5 text-xs leading-5 text-amber-100/80">
                <svg class="mt-0.5 h-4 w-4 shrink-0 text-amber-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                  <path d="M12 8v5m0 3.5v.5" stroke-linecap="round"/>
                  <circle cx="12" cy="12" r="9"/>
                </svg>
                <span>This video is over two hours long. The download can take a while and use significant disk space.</span>
              </p>
            </div>
          </article>

          <article class="rounded-2xl border border-white/[0.07] bg-ink-900 p-5 shadow-card sm:p-6">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h2 class="text-base font-semibold tracking-tight text-white">Choose a format</h2>
                <p id="format-summary" class="mt-1 text-xs text-zinc-400"></p>
              </div>
              <span id="ffmpeg-badge" hidden class="shrink-0 rounded-full border border-amber-300/20 bg-amber-300/[0.07] px-2.5 py-1 text-[10px] font-medium uppercase tracking-wider text-amber-200">ffmpeg off</span>
            </div>

            <div id="format-list" class="format-list mt-4 max-h-[23rem] space-y-2 overflow-y-auto pr-1" role="radiogroup" aria-label="Available download formats"></div>

            <div class="mt-5 border-t border-white/5 pt-5">
              <button
                id="download-btn"
                type="button"
                disabled
                class="inline-flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-accent px-5 text-sm font-semibold text-ink-950 transition duration-200 hover:bg-accent-soft focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-ink-900 disabled:cursor-not-allowed disabled:bg-ink-700 disabled:text-zinc-500"
              >
                <svg viewBox="0 0 24 24" class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M12 4v10m0 0 4-4m-4 4-4-4M5 19h14" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span id="download-btn-label">Start download</span>
              </button>
              <p id="format-hint" class="mt-2 text-center text-[11px] leading-5 text-zinc-400">Video-only formats are merged with the best audio track using ffmpeg.</p>

              <div id="job-panel" hidden class="mt-4 rounded-xl border border-white/10 bg-ink-850 p-4">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p id="job-status-label" class="text-sm font-medium text-white">Queued</p>
                    <p id="job-detail" class="mt-1 text-xs leading-5 text-zinc-400"></p>
                  </div>
                  <button
                    id="cancel-btn"
                    type="button"
                    class="shrink-0 rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-zinc-300 transition hover:border-rose-300/30 hover:bg-rose-300/[0.06] hover:text-rose-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-ink-850 disabled:cursor-not-allowed disabled:opacity-50"
                  >Cancel</button>
                </div>

                <div class="mt-4">
                  <div id="progress-track" class="relative h-2 overflow-hidden rounded-full bg-ink-700" role="progressbar" aria-label="Download progress" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                    <div id="progress-bar" class="h-full rounded-full bg-accent transition-[width] duration-300 ease-out" style="width: 0%"></div>
                  </div>
                  <div class="mt-2 flex items-start justify-between gap-3 text-[11px] text-zinc-400">
                    <span id="job-percent" class="font-mono text-zinc-300">0%</span>
                    <span id="job-metrics" class="text-right"></span>
                  </div>
                </div>

                <div id="job-error" hidden class="mt-4 rounded-lg border border-rose-400/20 bg-rose-400/[0.06] p-3">
                  <p id="job-error-message" class="text-xs leading-5 text-rose-100/80"></p>
                </div>

                <div id="job-success" hidden class="mt-4 flex flex-col gap-2 rounded-lg border border-emerald-400/20 bg-emerald-400/[0.06] p-3 sm:flex-row sm:items-center sm:justify-between">
                  <p class="text-xs leading-5 text-emerald-100/80">File is ready on the server.</p>
                  <a id="file-link" href="#" download class="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-lg bg-emerald-400/90 px-3 py-2 text-xs font-semibold text-emerald-950 transition hover:bg-emerald-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300 focus-visible:ring-offset-2 focus-visible:ring-offset-ink-850">
                    <svg viewBox="0 0 24 24" class="h-3.5 w-3.5" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">
                      <path d="M12 4v10m0 0 4-4m-4 4-4-4M5 19h14" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    Download file
                  </a>
                </div>

                <button id="retry-btn" type="button" hidden class="mt-3 rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-zinc-300 transition hover:border-accent/30 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-ink-850">Try again</button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <footer class="border-t border-white/5">
      <div class="mx-auto flex w-full max-w-6xl flex-col gap-2 px-5 py-6 text-xs text-zinc-400 sm:flex-row sm:items-center sm:justify-between sm:px-8">
        <p>ClipFlow wraps yt-dlp. Respect creators and platform terms.</p>
        <p class="font-mono text-zinc-400">FastAPI · yt-dlp · no build step</p>
      </div>
    </footer>
  </div>

  <script>
    "use strict";

    class ApiFailure extends Error {
      constructor(message, status, code) {
        super(message);
        this.name = "ApiFailure";
        this.status = status;
        this.code = code;
      }
    }

    const state = {
      metadata: null,
      selectedFormatId: null,
      jobId: null,
      pollTimer: null,
      pollFailures: 0,
      health: null,
      busy: false,
      jobTerminal: false,
    };

    const el = (id) => document.getElementById(id);
    const urlForm = el("url-form");
    const urlInput = el("url-input");
    const urlError = el("url-error");
    const fetchBtn = el("fetch-btn");
    const fetchLabel = el("fetch-label");
    const fetchSpinner = el("fetch-spinner");
    const emptyState = el("empty-state");
    const loadingState = el("loading-state");
    const metadataError = el("metadata-error");
    const metadataErrorCode = el("metadata-error-code");
    const metadataErrorMessage = el("metadata-error-message");
    const result = el("result");
    const workspace = el("workspace");
    const formatList = el("format-list");
    const formatSummary = el("format-summary");
    const formatCount = el("format-count");
    const formatHint = el("format-hint");
    const downloadBtn = el("download-btn");
    const downloadBtnLabel = el("download-btn-label");
    const jobPanel = el("job-panel");
    const jobStatusLabel = el("job-status-label");
    const jobDetail = el("job-detail");
    const cancelBtn = el("cancel-btn");
    const progressTrack = el("progress-track");
    const progressBar = el("progress-bar");
    const jobPercent = el("job-percent");
    const jobMetrics = el("job-metrics");
    const jobError = el("job-error");
    const jobErrorMessage = el("job-error-message");
    const jobSuccess = el("job-success");
    const fileLink = el("file-link");
    const retryBtn = el("retry-btn");
    const ffmpegBadge = el("ffmpeg-badge");
    const serverBanner = el("server-banner");
    const serverBannerText = el("server-banner-text");
    const serverChip = el("server-chip");
    const serverDot = el("server-dot");
    const serverText = el("server-text");

    function formatBytes(bytes) {
      if (typeof bytes !== "number" || !Number.isFinite(bytes) || bytes < 0) return "Size unknown";
      if (bytes === 0) return "0 B";
      const units = ["B", "KiB", "MiB", "GiB", "TiB"];
      const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
      const value = bytes / Math.pow(1024, exponent);
      return `${value >= 100 || exponent === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[exponent]}`;
    }

    function formatSpeed(bytesPerSecond) {
      if (typeof bytesPerSecond !== "number" || !Number.isFinite(bytesPerSecond) || bytesPerSecond <= 0) return null;
      return `${formatBytes(bytesPerSecond)}/s`;
    }

    function formatEta(seconds) {
      if (typeof seconds !== "number" || !Number.isFinite(seconds) || seconds < 0) return null;
      if (seconds < 60) return `${Math.max(1, Math.round(seconds))}s left`;
      const minutes = Math.floor(seconds / 60);
      const rest = Math.round(seconds % 60);
      if (minutes < 60) return `${minutes}m ${rest.toString().padStart(2, "0")}s left`;
      const hours = Math.floor(minutes / 60);
      return `${hours}h ${(minutes % 60).toString().padStart(2, "0")}m left`;
    }

    function formatDuration(seconds) {
      if (typeof seconds !== "number" || !Number.isFinite(seconds) || seconds <= 0) return "Unknown";
      const total = Math.round(seconds);
      const hours = Math.floor(total / 3600);
      const minutes = Math.floor((total % 3600) / 60);
      const secs = total % 60;
      if (hours > 0) return `${hours}:${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
      return `${minutes}:${secs.toString().padStart(2, "0")}`;
    }

    async function api(path, options = {}) {
      let response;
      try {
        response = await fetch(path, {
          headers: { "Accept": "application/json" },
          ...options,
        });
      } catch (error) {
        throw new ApiFailure("Couldn’t reach the ClipFlow server. Check that it is running.", 0, "network_error");
      }

      const contentType = response.headers.get("content-type") || "";
      let payload = null;
      if (contentType.includes("application/json")) {
        try {
          payload = await response.json();
        } catch (error) {
          payload = null;
        }
      }

      if (!response.ok) {
        const serverMessage = payload && payload.error && typeof payload.error.message === "string"
          ? payload.error.message
          : "The server couldn’t complete that request.";
        const code = payload && payload.error && typeof payload.error.code === "string"
          ? payload.error.code
          : "http_error";
        throw new ApiFailure(serverMessage, response.status, code);
      }

      if (payload === null) {
        throw new ApiFailure("The server returned an unexpected response.", response.status, "bad_response");
      }
      return payload;
    }

    function setUrlError(message) {
      if (message) {
        urlError.textContent = message;
        urlError.hidden = false;
        urlInput.setAttribute("aria-invalid", "true");
        urlInput.classList.add("border-rose-400/50");
      } else {
        urlError.textContent = "";
        urlError.hidden = true;
        urlInput.setAttribute("aria-invalid", "false");
        urlInput.classList.remove("border-rose-400/50");
      }
    }

    function validateClientUrl(raw) {
      const value = raw.trim();
      if (!value) return "Enter a video URL first.";
      if (value.length > 2048) return "That URL is too long.";
      let parsed;
      try {
        parsed = new URL(value);
      } catch (error) {
        return "Enter a complete http:// or https:// URL.";
      }
      if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
        return "Only http:// and https:// links are supported.";
      }
      if (parsed.username || parsed.password) {
        return "Links with embedded credentials aren’t allowed.";
      }
      if (!parsed.hostname) return "That URL is missing a hostname.";
      const host = parsed.hostname.toLowerCase();
      if (host === "localhost" || host.endsWith(".localhost")) {
        return "Local addresses aren’t allowed.";
      }
      return null;
    }

    function setFetching(isFetching) {
      state.busy = isFetching;
      fetchBtn.disabled = isFetching;
      fetchLabel.textContent = isFetching ? "Fetching…" : "Fetch formats";
      fetchSpinner.hidden = !isFetching;
      workspace.setAttribute("aria-busy", isFetching ? "true" : "false");
    }

    function resetForLookup() {
      stopPolling();
      state.metadata = null;
      state.selectedFormatId = null;
      state.jobId = null;
      state.jobTerminal = false;
      state.pollFailures = 0;
      jobPanel.hidden = true;
      jobError.hidden = true;
      jobSuccess.hidden = true;
      retryBtn.hidden = true;
      jobPanel.classList.remove("border-rose-400/20");
      setUrlError("");
      metadataError.hidden = true;
      result.hidden = true;
      emptyState.hidden = true;
      loadingState.hidden = false;
    }

    function showMetadataError(error) {
      loadingState.hidden = true;
      result.hidden = true;
      emptyState.hidden = true;
      metadataError.hidden = false;
      metadataErrorCode.textContent = error.code || "error";
      metadataErrorMessage.textContent = error.message;
    }

    function showEmptyState() {
      loadingState.hidden = true;
      result.hidden = true;
      metadataError.hidden = true;
      emptyState.hidden = false;
    }

    function compareVideoFormats(a, b) {
      const heightA = typeof a.height === "number" ? a.height : 0;
      const heightB = typeof b.height === "number" ? b.height : 0;
      if (heightA !== heightB) return heightB - heightA;
      if (a.has_audio !== b.has_audio) return a.has_audio ? -1 : 1;
      const fpsA = a.fps || 0;
      const fpsB = b.fps || 0;
      return fpsB - fpsA;
    }

    function compareAudioFormats(a, b) {
      return (b.abr || 0) - (a.abr || 0);
    }

    function ffmpegIsAvailable() {
      return !state.health || state.health.ffmpeg_available !== false;
    }

    function recommendedFormat(formats) {
      const usable = formats.filter((format) => !format.requires_merge || ffmpegIsAvailable());
      const videos = usable.filter((format) => format.has_video).sort(compareVideoFormats);
      if (videos.length > 0) return videos[0].id;
      const audio = usable.filter((format) => !format.has_video).sort(compareAudioFormats);
      return audio.length > 0 ? audio[0].id : null;
    }

    function addBadge(container, text, tone = "neutral", title = "") {
      const badge = document.createElement("span");
      const tones = {
        neutral: "border-white/10 bg-white/[0.04] text-zinc-400",
        accent: "border-accent/20 bg-accent/[0.08] text-accent-soft",
        amber: "border-amber-300/20 bg-amber-300/[0.07] text-amber-200",
        rose: "border-rose-300/20 bg-rose-300/[0.07] text-rose-200",
      };
      badge.className = `rounded-md border px-1.5 py-0.5 text-[10px] font-medium leading-4 ${tones[tone] || tones.neutral}`;
      badge.textContent = text;
      if (title) badge.title = title;
      container.appendChild(badge);
    }

    function buildFormatOption(format) {
      const missingFfmpeg = Boolean(format.requires_merge) && !ffmpegIsAvailable();
      const label = document.createElement("label");
      label.className = missingFfmpeg
        ? "format-option block cursor-not-allowed opacity-55"
        : "format-option block cursor-pointer";

      const input = document.createElement("input");
      input.type = "radio";
      input.name = "format";
      input.value = format.id;
      input.className = "peer sr-only";
      input.checked = format.id === state.selectedFormatId;
      input.disabled = missingFfmpeg;
      input.setAttribute("aria-describedby", `format-detail-${format.id}`);
      input.addEventListener("change", () => {
        if (input.checked) {
          state.selectedFormatId = format.id;
          downloadBtn.disabled = false;
          downloadBtnLabel.textContent = "Start download";
          formatHint.textContent = format.requires_merge
            ? "This format has no audio. ClipFlow will merge it with the best audio track using ffmpeg."
            : format.has_audio
              ? "This format already includes both video and audio."
              : "This format is audio-only.";
        }
      });

      const card = document.createElement("div");
      card.className = "format-card rounded-xl border border-white/10 bg-ink-850 p-3 transition duration-200 peer-checked:border-accent/60 peer-checked:bg-accent/[0.07] peer-focus-visible:ring-2 peer-focus-visible:ring-accent peer-focus-visible:ring-offset-2 peer-focus-visible:ring-offset-ink-900 peer-hover:border-white/20";

      const top = document.createElement("div");
      top.className = "flex items-center justify-between gap-3";
      const title = document.createElement("div");
      title.className = "flex min-w-0 items-center gap-2";

      const dot = document.createElement("span");
      dot.className = "format-dot flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-full border border-zinc-600 transition-colors";
      dot.innerHTML = '<span class="h-1.5 w-1.5 rounded-full bg-accent opacity-0 transition-opacity"></span>';

      const name = document.createElement("span");
      name.className = "truncate text-sm font-medium text-zinc-200";
      name.textContent = format.resolution;

      title.appendChild(dot);
      title.appendChild(name);

      const size = document.createElement("span");
      size.className = "shrink-0 font-mono text-[11px] text-zinc-400";
      size.textContent = format.filesize ? formatBytes(format.filesize) : "Size unknown";

      top.appendChild(title);
      top.appendChild(size);

      const meta = document.createElement("div");
      meta.id = `format-detail-${format.id}`;
      meta.className = "mt-2 flex flex-wrap items-center gap-1.5 pl-5";

      addBadge(meta, format.ext ? format.ext.toUpperCase() : "?", "accent");
      if (format.has_video && format.has_audio) addBadge(meta, "video + audio");
      if (format.has_video && !format.has_audio) addBadge(meta, "video only", "amber");
      if (!format.has_video && format.has_audio) addBadge(meta, "audio only");
      if (format.video_codec) addBadge(meta, format.video_codec, "neutral", format.vcodec || "");
      if (format.audio_codec) addBadge(meta, format.audio_codec, "neutral", format.acodec || "");
      if (format.fps) addBadge(meta, `${format.fps} fps`);
      if (format.requires_merge && !missingFfmpeg) addBadge(meta, "merge with ffmpeg", "amber");
      if (missingFfmpeg) addBadge(meta, "ffmpeg missing", "rose");
      const idBadge = document.createElement("span");
      idBadge.className = "font-mono text-[10px] text-zinc-400";
      idBadge.textContent = `id ${format.id}`;
      meta.appendChild(idBadge);

      card.appendChild(top);
      card.appendChild(meta);
      label.appendChild(input);
      label.appendChild(card);
      return label;
    }

    function renderFormats(metadata) {
      const formats = metadata.formats.slice();
      const videos = formats.filter((format) => format.has_video).sort(compareVideoFormats);
      const audio = formats.filter((format) => !format.has_video).sort(compareAudioFormats);
      const ordered = videos.concat(audio);
      const recommendedId = recommendedFormat(ordered);

      state.selectedFormatId = recommendedId;
      formatList.replaceChildren();
      for (const format of ordered) {
        formatList.appendChild(buildFormatOption(format));
      }

      const withAudio = formats.filter((format) => format.has_audio).length;
      const mergeCount = formats.filter((format) => format.requires_merge).length;
      formatCount.textContent = `${formats.length} format${formats.length === 1 ? "" : "s"}`;
      formatSummary.textContent = `${withAudio} with audio · ${mergeCount} video-only${ffmpegIsAvailable() ? "" : " (needs ffmpeg)"}`;

      if (recommendedId) {
        const selected = formats.find((format) => format.id === recommendedId);
        downloadBtn.disabled = false;
        downloadBtnLabel.textContent = "Start download";
        if (selected && selected.requires_merge) {
          formatHint.textContent = "Recommended format is video-only; ClipFlow will merge it with the best audio using ffmpeg.";
        } else if (selected && selected.has_audio && selected.has_video) {
          formatHint.textContent = "Recommended format already includes both video and audio.";
        } else {
          formatHint.textContent = "Recommended format is audio-only.";
        }
      } else {
        downloadBtn.disabled = true;
        downloadBtnLabel.textContent = "No usable format";
        formatHint.textContent = "No format can be downloaded without ffmpeg. Install ffmpeg on the server, then reload.";
      }

      if (!ffmpegIsAvailable()) {
        ffmpegBadge.hidden = false;
      } else {
        ffmpegBadge.hidden = true;
      }
    }

    function renderMetadata(metadata) {
      state.metadata = metadata;
      state.selectedFormatId = null;
      el("video-title").textContent = metadata.title || "Untitled video";
      el("uploader").textContent = metadata.uploader || "Unknown uploader";
      el("duration-badge").textContent = formatDuration(metadata.duration);
      let sourceHost = "yt-dlp source";
      try {
        sourceHost = new URL(metadata.url).hostname.replace(/^www\./, "");
      } catch (error) {
        sourceHost = "yt-dlp source";
      }
      el("source-label").textContent = sourceHost;
      const thumb = el("thumbnail");
      const fallback = el("thumb-fallback");
      thumb.alt = metadata.title ? `Thumbnail for ${metadata.title}` : "Video thumbnail";
      if (metadata.thumbnail) {
        let retried = false;
        thumb.hidden = false;
        fallback.hidden = true;
        thumb.onerror = () => {
          if (!retried) {
            retried = true;
            try {
              const retryUrl = new URL(metadata.thumbnail);
              retryUrl.searchParams.set("clipflow_retry", "1");
              thumb.src = retryUrl.toString();
              return;
            } catch (error) {
              retried = true;
            }
          }
          thumb.hidden = true;
          fallback.hidden = false;
        };
        thumb.src = metadata.thumbnail;
      } else {
        thumb.removeAttribute("src");
        thumb.hidden = true;
        fallback.hidden = false;
      }
      el("long-warning").hidden = !metadata.is_long;
      renderFormats(metadata);
      loadingState.hidden = true;
      emptyState.hidden = true;
      metadataError.hidden = true;
      result.hidden = false;
    }

    async function cancelActiveJob({ quiet = false } = {}) {
      const jobId = state.jobId;
      stopPolling();
      state.jobId = null;
      state.jobTerminal = false;
      if (!jobId) return;
      try {
        await api(`/api/downloads/${encodeURIComponent(jobId)}/cancel`, { method: "POST" });
      } catch (error) {
        if (!quiet && error.status !== 409 && error.status !== 404) {
          console.warn("Could not cancel the previous job:", error.message);
        }
      }
    }

    async function runLookup(rawUrl) {
      const clientError = validateClientUrl(rawUrl);
      if (clientError) {
        setUrlError(clientError);
        urlInput.focus();
        return;
      }
      setUrlError("");
      await cancelActiveJob({ quiet: true });
      resetForLookup();
      setFetching(true);

      try {
        const metadata = await api("/api/metadata", {
          method: "POST",
          headers: { "Content-Type": "application/json", "Accept": "application/json" },
          body: JSON.stringify({ url: rawUrl.trim() }),
        });
        renderMetadata(metadata);
      } catch (error) {
        showMetadataError(error);
      } finally {
        setFetching(false);
      }
    }

    function setJobStatus(status) {
      const labels = {
        queued: "Queued",
        downloading: "Downloading",
        processing: "Processing",
        completed: "Ready",
        failed: "Failed",
        cancelled: "Cancelled",
      };
      jobStatusLabel.textContent = labels[status] || status;
    }

    function setProgress(job) {
      const hasPercent = typeof job.percent === "number" && Number.isFinite(job.percent);
      const percent = hasPercent ? Math.max(0, Math.min(100, job.percent)) : null;
      const active = ["queued", "downloading", "processing"].includes(job.status);
      if (percent === null) {
        progressBar.style.width = "0%";
        progressBar.classList.add("hidden");
        progressTrack.classList.toggle("indeterminate", active);
        progressTrack.removeAttribute("aria-valuenow");
        progressTrack.setAttribute("aria-valuetext", active ? "Downloading, percentage unknown" : "No progress recorded");
        jobPercent.textContent = "—";
      } else {
        progressBar.classList.remove("hidden");
        progressTrack.classList.remove("indeterminate");
        progressBar.style.width = `${percent}%`;
        progressTrack.setAttribute("aria-valuenow", String(Math.round(percent)));
        progressTrack.removeAttribute("aria-valuetext");
        jobPercent.textContent = `${percent.toFixed(1)}%`;
      }

      const metrics = [];
      if (job.status === "downloading") {
        const speed = formatSpeed(job.speed);
        if (speed) metrics.push(speed);
        const eta = formatEta(job.eta);
        if (eta) metrics.push(eta);
        if (typeof job.downloaded_bytes === "number" && typeof job.total_bytes === "number" && job.total_bytes > 0) {
          metrics.push(`${formatBytes(job.downloaded_bytes)} of ${formatBytes(job.total_bytes)}`);
        } else if (typeof job.downloaded_bytes === "number") {
          metrics.push(`${formatBytes(job.downloaded_bytes)} downloaded`);
        }
        jobDetail.textContent = "Transferring from the source.";
      } else if (job.status === "processing") {
        jobDetail.textContent = "Merging or finalizing the file with ffmpeg.";
        metrics.push("finalizing");
      } else if (job.status === "queued") {
        jobDetail.textContent = "Waiting for the yt-dlp worker to start.";
      } else if (job.status === "completed") {
        jobDetail.textContent = "The finished file is stored on the server.";
        if (typeof job.total_bytes === "number") metrics.push(formatBytes(job.total_bytes));
      } else if (job.status === "cancelled") {
        jobDetail.textContent = "The yt-dlp process was stopped.";
      } else if (job.status === "failed") {
        jobDetail.textContent = "yt-dlp could not finish this download.";
      }
      jobMetrics.textContent = metrics.join(" · ");
    }

    function updateJobUI(job) {
      setJobStatus(job.status);
      setProgress(job);
      state.jobTerminal = ["completed", "failed", "cancelled"].includes(job.status);

      const active = ["queued", "downloading", "processing"].includes(job.status);
      cancelBtn.hidden = !active;
      cancelBtn.disabled = false;
      cancelBtn.textContent = "Cancel";
      downloadBtn.disabled = active;
      downloadBtnLabel.textContent = active
        ? "Download in progress"
        : job.status === "completed"
          ? "Download again"
          : "Retry download";

      jobError.hidden = job.status !== "failed" && job.status !== "cancelled";
      if (job.status === "failed") {
        jobErrorMessage.textContent = job.error || "The download failed. Try again.";
        retryBtn.hidden = false;
        retryBtn.textContent = "Try again";
      } else if (job.status === "cancelled") {
        jobErrorMessage.textContent = "Download cancelled. No partial file was kept.";
        retryBtn.hidden = false;
        retryBtn.textContent = "Start again";
      } else {
        jobErrorMessage.textContent = "";
        retryBtn.hidden = true;
      }

      jobSuccess.hidden = job.status !== "completed";
      if (job.status === "completed") {
        const downloadUrl = job.download_url || `/api/downloads/${encodeURIComponent(job.job_id || state.jobId)}/file`;
        fileLink.href = downloadUrl;
        fileLink.setAttribute("download", job.filename || "clipflow-download");
        fileLink.title = job.filename || "Download the finished file";
        fileLink.textContent = "";
        const icon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        icon.setAttribute("viewBox", "0 0 24 24");
        icon.setAttribute("class", "h-3.5 w-3.5");
        icon.setAttribute("fill", "none");
        icon.setAttribute("stroke", "currentColor");
        icon.setAttribute("stroke-width", "2.2");
        icon.setAttribute("aria-hidden", "true");
        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.setAttribute("d", "M12 4v10m0 0 4-4m-4 4-4-4M5 19h14");
        path.setAttribute("stroke-linecap", "round");
        path.setAttribute("stroke-linejoin", "round");
        icon.appendChild(path);
        fileLink.appendChild(icon);
        fileLink.appendChild(document.createTextNode("Download file"));
      } else {
        fileLink.href = "#";
        fileLink.removeAttribute("download");
        fileLink.removeAttribute("title");
      }
    }

    function stopPolling() {
      if (state.pollTimer !== null) {
        clearTimeout(state.pollTimer);
        state.pollTimer = null;
      }
    }

    async function pollJob() {
      const jobId = state.jobId;
      if (!jobId) return;
      try {
        const job = await api(`/api/downloads/${encodeURIComponent(jobId)}`);
        if (state.jobId !== jobId) return;
        state.pollFailures = 0;
        updateJobUI(job);
        if (["completed", "failed", "cancelled"].includes(job.status)) {
          stopPolling();
          return;
        }
        state.pollTimer = setTimeout(pollJob, 800);
      } catch (error) {
        if (state.jobId !== jobId) return;
        if (error.status === 404) {
          stopPolling();
          state.jobTerminal = true;
          downloadBtn.disabled = false;
          downloadBtnLabel.textContent = "Retry download";
          jobStatusLabel.textContent = "Lost";
          jobDetail.textContent = "This job is no longer known to the server.";
          jobError.hidden = false;
          jobErrorMessage.textContent = error.message;
          retryBtn.hidden = false;
          return;
        }
        state.pollFailures += 1;
        if (state.pollFailures >= 6) {
          stopPolling();
          jobStatusLabel.textContent = "Connection lost";
          jobDetail.textContent = "Couldn’t reach the server to check this download.";
          jobError.hidden = false;
          jobErrorMessage.textContent = "The job may still be running on the server. Start a new request once the server is reachable.";
          retryBtn.hidden = true;
          downloadBtn.disabled = true;
          return;
        }
        jobDetail.textContent = "Reconnecting to check progress…";
        state.pollTimer = setTimeout(pollJob, Math.min(5000, 900 * Math.pow(2, state.pollFailures)));
      }
    }

    async function startDownload() {
      if (!state.metadata || !state.selectedFormatId) return;
      if (state.busy) return;
      stopPolling();
      state.pollFailures = 0;
      state.jobTerminal = false;
      state.jobId = null;
      jobPanel.hidden = false;
      jobSuccess.hidden = true;
      jobError.hidden = true;
      retryBtn.hidden = true;
      jobStatusLabel.textContent = "Starting";
      jobDetail.textContent = "Sending the request to yt-dlp.";
      jobMetrics.textContent = "";
      jobPercent.textContent = "—";
      progressBar.style.width = "0%";
      progressTrack.classList.remove("indeterminate");
      downloadBtn.disabled = true;
      downloadBtnLabel.textContent = "Starting…";
      cancelBtn.hidden = true;

      try {
        const created = await api("/api/downloads", {
          method: "POST",
          headers: { "Content-Type": "application/json", "Accept": "application/json" },
          body: JSON.stringify({
            url: state.metadata.url,
            format_id: state.selectedFormatId,
          }),
        });
        state.jobId = created.job_id;
        jobPanel.hidden = false;
        await pollJob();
      } catch (error) {
        jobPanel.hidden = false;
        jobStatusLabel.textContent = "Not started";
        jobDetail.textContent = "The download request was rejected.";
        jobError.hidden = false;
        jobErrorMessage.textContent = error.message;
        retryBtn.hidden = false;
        retryBtn.textContent = "Try again";
        downloadBtn.disabled = false;
        downloadBtnLabel.textContent = "Start download";
      }
    }

    urlForm.addEventListener("submit", (event) => {
      event.preventDefault();
      if (state.busy) return;
      runLookup(urlInput.value);
    });

    urlInput.addEventListener("input", () => {
      if (!urlError.hidden) setUrlError("");
    });

    downloadBtn.addEventListener("click", () => {
      if (downloadBtn.disabled) return;
      if (state.jobTerminal) {
        jobError.hidden = true;
        jobSuccess.hidden = true;
      }
      startDownload();
    });

    cancelBtn.addEventListener("click", async () => {
      if (!state.jobId) return;
      const jobId = state.jobId;
      cancelBtn.disabled = true;
      cancelBtn.textContent = "Cancelling…";
      try {
        const job = await api(`/api/downloads/${encodeURIComponent(jobId)}/cancel`, { method: "POST" });
        if (state.jobId === jobId) {
          updateJobUI(job);
          stopPolling();
        }
      } catch (error) {
        if (state.jobId === jobId) {
          cancelBtn.disabled = false;
          cancelBtn.textContent = "Cancel";
          jobDetail.textContent = error.message;
        }
      }
    });

    retryBtn.addEventListener("click", () => {
      retryBtn.hidden = true;
      jobError.hidden = true;
      startDownload();
    });

    async function loadHealth() {
      try {
        const health = await api("/api/health");
        state.health = health;
        serverChip.className = "inline-flex items-center gap-2 rounded-full border border-emerald-400/20 bg-emerald-400/[0.05] px-3 py-1.5 text-xs font-medium text-emerald-200";
        serverDot.className = "h-1.5 w-1.5 rounded-full bg-emerald-400";
        serverText.textContent = "Server ready";
        const problems = [];
        if (!health.yt_dlp_available) {
          problems.push("yt-dlp isn’t installed on the server. Format fetching and downloads won’t work until it is.");
        }
        if (!health.ffmpeg_available) {
          problems.push("ffmpeg isn’t installed. Video-only formats can’t be merged with audio; formats that already include audio still work.");
        }
        if (problems.length > 0) {
          serverBanner.hidden = false;
          serverBannerText.textContent = problems.join(" ");
        }
      } catch (error) {
        serverChip.className = "inline-flex items-center gap-2 rounded-full border border-rose-400/20 bg-rose-400/[0.05] px-3 py-1.5 text-xs font-medium text-rose-200";
        serverDot.className = "h-1.5 w-1.5 rounded-full bg-rose-400";
        serverText.textContent = "Server offline";
        serverBanner.hidden = false;
        serverBannerText.textContent = error.message;
      }
    }

    loadHealth();
  </script>
</body>
</html>

``````

### `clipflow/pytest.ini`

``````text
[pytest]
testpaths = tests
addopts = -q

``````

### `clipflow/requirements.txt`

``````text
fastapi>=0.115,<1.0
uvicorn>=0.34,<1.0
pydantic>=2.10,<3.0
yt-dlp>=2025.1.15
pytest>=8.3,<9.0
httpx>=0.28,<1.0

``````

### `clipflow/tests/test_app.py`

``````text
import json
import subprocess

import pytest
from fastapi.testclient import TestClient

import app as clipflow


PUBLIC_URL = "https://www.youtube.com/watch?v=clipflow-test"


@pytest.fixture(autouse=True)
def isolated_state(tmp_path, monkeypatch):
    monkeypatch.setattr(clipflow, "DOWNLOAD_ROOT", tmp_path / "downloads")
    clipflow.DOWNLOAD_ROOT.mkdir(parents=True, exist_ok=True)
    monkeypatch.setattr(clipflow, "_resolve_host", lambda host, port: ["93.184.216.34"])
    with clipflow.CACHE_LOCK:
        clipflow.METADATA_CACHE.clear()
    with clipflow.HOST_CACHE_LOCK:
        clipflow.HOST_CACHE.clear()
    clipflow.manager.reset()
    yield
    clipflow.manager.reset()
    with clipflow.CACHE_LOCK:
        clipflow.METADATA_CACHE.clear()
    with clipflow.HOST_CACHE_LOCK:
        clipflow.HOST_CACHE.clear()


@pytest.fixture
def client():
    with TestClient(clipflow.app) as test_client:
        yield test_client


def test_health_returns_ok(client):
    response = client.get("/api/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["service"] == "clipflow"
    assert body["max_active_jobs"] == 3


def test_url_validation_rejects_non_http_and_private_addresses(client, monkeypatch):
    def fail_if_called(*args, **kwargs):
        raise AssertionError("yt-dlp must not run for a rejected URL")

    monkeypatch.setattr(clipflow.subprocess, "run", fail_if_called)
    bad_urls = [
        "ftp://example.com/video.mp4",
        "javascript:alert(1)",
        "file:///etc/passwd",
        "http://127.0.0.1/admin",
        "http://10.0.0.1/internal",
        "http://192.168.1.20/video",
        "http://169.254.169.254/latest/meta-data",
        "http://localhost/video",
        "http://metadata.google.internal/computeMetadata/v1/",
        "http://[::1]/video",
    ]
    for url in bad_urls:
        response = client.post("/api/metadata", json={"url": url})
        assert response.status_code == 400, url
        assert response.json()["error"]["code"] in {
            "invalid_url",
            "blocked_url",
            "unresolvable_url",
        }


def test_metadata_parses_ytdlp_formats(client, monkeypatch):
    monkeypatch.setattr(clipflow, "_ytdlp_path", lambda: "/bin/sh")
    fake_info = {
        "title": "A Real Parsed Title",
        "uploader": "Channel Name",
        "duration": 65,
        "thumbnail": "https://i.ytimg.com/vi/example/hqdefault.jpg",
        "formats": [
            {
                "format_id": "18",
                "ext": "mp4",
                "resolution": "640x360",
                "height": 360,
                "filesize": 1234,
                "vcodec": "avc1.42001E",
                "acodec": "mp4a.40.2",
                "protocol": "https",
            },
            {
                "format_id": "137",
                "ext": "mp4",
                "resolution": "1920x1080",
                "height": 1080,
                "filesize_approx": 9999,
                "vcodec": "avc1.640028",
                "acodec": "none",
                "protocol": "https",
            },
            {
                "format_id": "140",
                "ext": "m4a",
                "resolution": "audio only",
                "vcodec": "none",
                "acodec": "mp4a.40.2",
                "abr": 128,
                "protocol": "https",
            },
            {
                "format_id": "storyboard",
                "ext": "mhtml",
                "vcodec": "none",
                "acodec": "none",
                "protocol": "mhtml",
            },
        ],
    }

    def fake_run(command, **kwargs):
        assert "--dump-single-json" in command
        assert PUBLIC_URL in command
        return subprocess.CompletedProcess(command, 0, stdout=json.dumps(fake_info), stderr="")

    monkeypatch.setattr(clipflow.subprocess, "run", fake_run)
    response = client.post("/api/metadata", json={"url": PUBLIC_URL})
    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "A Real Parsed Title"
    assert body["uploader"] == "Channel Name"
    assert body["duration"] == 65
    assert len(body["formats"]) == 3
    by_id = {item["id"]: item for item in body["formats"]}
    assert by_id["18"]["has_video"] is True
    assert by_id["18"]["has_audio"] is True
    assert by_id["18"]["vcodec"] == "avc1.42001E"
    assert by_id["18"]["acodec"] == "mp4a.40.2"
    assert by_id["18"]["video_codec"] == "H.264"
    assert by_id["18"]["audio_codec"] == "AAC"
    assert by_id["137"]["requires_merge"] is True
    assert by_id["137"]["video_codec"] == "H.264"
    assert by_id["137"]["audio_codec"] is None
    assert by_id["140"]["has_video"] is False
    assert by_id["140"]["has_audio"] is True
    assert by_id["140"]["vcodec"] is None
    assert by_id["140"]["audio_codec"] == "AAC"


def test_filename_sanitization_and_file_serving_are_contained(client, monkeypatch, tmp_path):
    assert clipflow.sanitize_filename("../../etc/passwd") == "passwd"
    assert clipflow.sanitize_filename("..\\..\\evil.mp4") == "evil.mp4"
    assert clipflow.sanitize_filename("normal clip.mp4") == "normal clip.mp4"
    assert "/" not in clipflow.sanitize_filename("../../outside.mp4")
    assert not clipflow._is_inside(tmp_path / "outside.mp4", clipflow.DOWNLOAD_ROOT)

    monkeypatch.setattr(clipflow, "_start_worker", lambda job: None)
    created = client.post(
        "/api/downloads",
        json={"url": PUBLIC_URL, "format_id": "18"},
    )
    assert created.status_code == 202
    job_id = created.json()["job_id"]
    secret = tmp_path / "secret.mp4"
    secret.write_bytes(b"not yours")
    job = clipflow.manager.get(job_id)
    with clipflow.manager.lock:
        job.status = "completed"
        job.file_path = str(secret)
    response = client.get(f"/api/downloads/{job_id}/file")
    assert response.status_code == 404
    assert response.json()["error"]["code"] == "file_not_found"


def test_create_download_returns_job_id_and_bounds_concurrency(client, monkeypatch):
    monkeypatch.setattr(clipflow, "_start_worker", lambda job: None)
    created_ids = []
    for _ in range(3):
        response = client.post(
            "/api/downloads",
            json={"url": PUBLIC_URL, "format_id": "18"},
        )
        assert response.status_code == 202
        body = response.json()
        assert isinstance(body["job_id"], str) and body["job_id"]
        created_ids.append(body["job_id"])

    status_response = client.get(f"/api/downloads/{created_ids[0]}")
    assert status_response.status_code == 200
    assert status_response.json()["status"] == "queued"

    fourth = client.post(
        "/api/downloads",
        json={"url": PUBLIC_URL, "format_id": "18"},
    )
    assert fourth.status_code == 429
    assert fourth.json()["error"]["code"] == "too_many_jobs"


def test_cancel_transitions_state(client, monkeypatch):
    monkeypatch.setattr(clipflow, "_start_worker", lambda job: None)
    created = client.post(
        "/api/downloads",
        json={"url": PUBLIC_URL, "format_id": "18"},
    )
    assert created.status_code == 202
    job_id = created.json()["job_id"]

    cancelled = client.post(f"/api/downloads/{job_id}/cancel")
    assert cancelled.status_code == 200
    assert cancelled.json()["status"] == "cancelled"

    status_response = client.get(f"/api/downloads/{job_id}")
    assert status_response.status_code == 200
    assert status_response.json()["status"] == "cancelled"


def test_unknown_job_returns_404(client):
    status_response = client.get("/api/downloads/does-not-exist")
    assert status_response.status_code == 404
    assert status_response.json()["error"]["code"] == "job_not_found"

    cancel_response = client.post("/api/downloads/does-not-exist/cancel")
    assert cancel_response.status_code == 404

    file_response = client.get("/api/downloads/does-not-exist/file")
    assert file_response.status_code == 404


def test_malformed_body_returns_400(client):
    bad_json = client.post(
        "/api/metadata",
        content='{"url": ',
        headers={"Content-Type": "application/json"},
    )
    assert bad_json.status_code == 400
    assert bad_json.json()["error"]["code"] == "invalid_request"

    missing_field = client.post("/api/metadata", json={})
    assert missing_field.status_code == 400
    assert missing_field.json()["error"]["code"] == "invalid_request"

    missing_format = client.post("/api/downloads", json={"url": PUBLIC_URL})
    assert missing_format.status_code == 400
    assert missing_format.json()["error"]["code"] == "invalid_request"

``````
