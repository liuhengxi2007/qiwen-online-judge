# Problem Data Storage Migration

This document records the agreed migration path for problem data storage.

Most of the planned migration has now been implemented. See also [Problem Data Operations](./problem-data-operations.md) for the current runtime state.

The target design moves problem data to object storage compatible with MinIO/S3, supports tree-shaped relative paths, and removes large base64 payloads from storage-oriented protocols.

## Goals

- support tree-shaped problem data paths such as `samples/1.in` and `tests/sub1/a.out`
- store problem data in MinIO/S3-style object storage
- move management upload APIs to binary HTTP uploads instead of JSON base64 payloads
- stop embedding testcase bytes in judge-task JSON payloads
- keep authorization in the backend boundary

## Phase 1

Phase 1 starts by extracting a reusable upload capability and then reusing it from problem data.

- introduce shared upload path and upload-preparation utilities
- keep line-ending normalization policy-driven and disabled by default

## Phase 2

Phase 2 is a problem-data-specific backend refactor with compatibility preserved.

- introduce a `ProblemDataPath` model for validated relative paths
- refactor problem data storage behind a replaceable storage port
- keep the current HTTP JSON fields and local filesystem behavior working
- make the local filesystem implementation path-aware so zip extraction and future tree views do not require another storage rewrite

This phase does not yet require frontend or judger protocol changes.

Status: implemented

## Phase 3

Replace the current management upload API with binary upload endpoints.

Recommended routes:

- `POST /api/problems/{slug}/data/files`
- `GET /api/problems/{slug}/data/files`
- `POST /api/problems/{slug}/data/archive-imports`
- `GET /api/problems/{slug}/data/files/tree`
- `GET /api/problems/{slug}/data/files/download?path=...`
- `POST /api/problems/{slug}/data/files/delete`
- `POST /api/problems/{slug}/data/files/delete-all`
- `POST /api/problems/{slug}/data/ready-state`

Recommended request shape for single-file upload:

- `multipart/form-data`
- field `file`
- field `path`
- optional field `replace`

Status: implemented

## Phase 4

Introduce MinIO-backed storage and file metadata persistence.

Recommended object key pattern:

- `problems/{problemSlug}/data/{relativePath}`

Implemented table:

- `problem_data_file`
- current columns: `problem_id`, `relative_path`, `size_bytes`, `sha256`, `created_at`
- future extensions may add `object_key` and `etag` if storage metadata needs to be persisted explicitly

The existing `problems.data_name` field should become a compatibility summary field and later be removed or downgraded in importance.

Status: partially implemented

## Phase 5

Change judge-task transport so testcase data is referenced, not embedded.

Recommended shapes:

- object references by storage key for trusted internal deployments
- signed download URLs for decoupled judger deployments

Avoid continuing with `inputBase64` and `expectedOutputBase64` once object storage is the source of truth.

The first implementation step may use backend-internal authenticated downloads instead of direct MinIO access from judgers.

Recommended cache shape on the judger:

- blob cache keyed by `sha256`
- lightweight manifest records keyed by `problemDataVersion`

Status: implemented with backend-internal downloads and basic cache files. Direct MinIO judger access and more advanced cache eviction remain optional future work.

## Constraints

- all paths must be relative
- reject `.` and `..` path segments
- normalize separators to `/`
- do not model empty directories as durable resources
- backend remains responsible for authorization checks

## Notes

- The local storage implementation still needs compensating cleanup because database transactions do not extend to object storage.
- Zip extraction must guard against path traversal and unreasonable expansion.
- Judge testcase discovery should eventually become prefix-based, for example `tests/`, instead of scanning every uploaded file.
