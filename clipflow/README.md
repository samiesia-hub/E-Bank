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
