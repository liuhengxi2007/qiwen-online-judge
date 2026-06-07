# Problem Data Operations

This document records the current implemented state of problem-data storage and the runtime configuration needed to operate it.

## Current State

The repository currently implements:

- reusable backend upload preparation utilities under `shared.application.upload`
- multipart single-file uploads for problem data
- multipart zip uploads for problem data
- tree-shaped problem-data listing for management pages
- path-aware file download and deletion for problem data
- zip archive downloads for all current problem data
- mandatory MinIO-backed storage in the backend
- `problem_data_files` metadata persistence in PostgreSQL
- judge-task transport by file reference instead of embedded base64 payloads
- judger-side blob cache keyed by `sha256`
- backend-internal authenticated problem-data downloads for judgers

The repository intentionally still uses backend-mediated judger downloads instead of direct MinIO access from judgers.

## Backend Configuration

### MinIO Storage

- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`
- `MINIO_SECURE`
  Optional boolean-like flag
  Default: `true`

The endpoint, credentials, and bucket are required. Backend startup fails if any required MinIO value is missing or blank. The same MinIO configuration is used for problem data and stored submission source programs.

## Judger Configuration

### Cache

- `JUDGER_PROBLEM_DATA_CACHE_ROOT`
  Root directory for judger problem-data cache
  Default: `<JUDGER_WORK_ROOT>/problem-data-cache`

The current cache layout is:

- `blobs/<sha256>`
- `manifests/<problemSlug>-<problemDataVersion>.txt`

### Existing Judger Settings Still In Use

- `BACKEND_BASE_URL`
- `JUDGE_TOKEN`
- `JUDGER_ID_PREFIX`
- `POLL_INTERVAL_MS`
- `CXX`
- `PYTHON3`
- `ISOLATE_BIN`
- `ISOLATE_BOX_ID`
- `ISOLATE_PREFER_CGROUPS`
- `JUDGER_WORK_ROOT`

## HTTP Surface

### Problem Data Management

- `POST /api/problems/{slug}/data/files`
  Multipart single-file upload with fields:
  - `file`
  - optional `path`

- `GET /api/problems/{slug}/data/files`
  Returns the flat filename list for compatibility with existing problem-data summaries.

- `POST /api/problems/{slug}/data/archive-imports`
  Multipart zip upload with fields:
  - `file`
  - optional `targetDir`

- `GET /api/problems/{slug}/data/files/tree`
  Returns flat tree nodes for files and directories.

- `GET /api/problems/{slug}/data/files/download?path=...`
  Path-aware file download.

- `GET /api/problems/{slug}/data/archive-downloads`
  Downloads a zip archive containing all current problem-data files. Entry names preserve the stored relative paths exactly, such as `judge.yaml` and `cases/1.in`; the archive does not add an outer `{slug}/` directory. An empty problem-data manifest returns a valid empty zip.

- `POST /api/problems/{slug}/data/files/delete`
  JSON body with `path`.

- `POST /api/problems/{slug}/data/files/delete-all`
  Deletes all uploaded problem-data files.

- `POST /api/problems/{slug}/data/ready-state`
  JSON body with `ready`.

### Judger Worker Access

- `GET /api/worker/judge/problem-data?problemSlug=...&path=...`
  Judge-token-protected endpoint used by judgers to fill cache entries.

## Operational Notes

- `problem_data_files` is the current source of truth for file metadata and manifest construction.
- Stored bytes are still the source of truth for actual downloads and sandbox execution.
- `problems.data_name` remains as a compatibility summary field.
- The old JSON base64 upload endpoint has been removed.
- Tree listing and path-aware deletion/download are intended for authenticated management flows, not public problem consumption.
