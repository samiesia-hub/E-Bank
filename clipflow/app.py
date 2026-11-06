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
