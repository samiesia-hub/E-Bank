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
