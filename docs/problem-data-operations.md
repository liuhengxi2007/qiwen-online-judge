# Problem Data Operations

This document records the current implemented state of problem-data storage and the runtime configuration needed to operate it.

## Current State

The repository currently implements:

- reusable backend upload preparation utilities under `domains.shared.upload`
- multipart single-file uploads for problem data
- multipart zip uploads for problem data
- tree-shaped problem-data listing for management pages
- path-aware file download and deletion for problem data
- local or MinIO-backed storage selection in the backend
- `problem_data_files` metadata persistence in PostgreSQL
- judge-task transport by file reference instead of embedded base64 payloads
- judger-side blob cache keyed by `sha256`
- backend-internal authenticated problem-data downloads for judgers

The repository intentionally still uses backend-mediated judger downloads instead of direct MinIO access from judgers.

## Backend Configuration

### Storage Backend Selection

- `PROBLEM_DATA_STORAGE_BACKEND`
  Values: `local` or `minio`
  Default: `local`

### Local Storage

- `PROBLEM_DATA_LOCAL_ROOT`
  Optional absolute or relative path used when the backend storage backend is `local`
  Default: `<backend working directory>/problems`

### MinIO Storage

- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`
- `MINIO_SECURE`
  Optional boolean-like flag
  Default: `true`

When `PROBLEM_DATA_STORAGE_BACKEND=minio`, the endpoint, credentials, and bucket are required.

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

- `POST /api/problems/{slug}/data/archive`
  Multipart zip upload with fields:
  - `file`
  - optional `targetDir`

- `GET /api/problems/{slug}/data/tree`
  Returns flat tree nodes for files and directories.

- `GET /api/problems/{slug}/data/file?path=...`
  Path-aware file download.

- `POST /api/problems/{slug}/data/file/delete`
  JSON body with `path`.

### Judger Internal Access

- `GET /api/internal/judge/problem-data?problemSlug=...&path=...`
  Judge-token-protected endpoint used by judgers to fill cache entries.

## Operational Notes

- `problem_data_files` is the current source of truth for file metadata and manifest construction.
- Stored bytes are still the source of truth for actual downloads and sandbox execution.
- `problems.data_name` remains as a compatibility summary field.
- The old JSON base64 upload endpoint has been removed.
- Tree listing and path-aware deletion/download are intended for authenticated management flows, not public problem consumption.
