# File Upload Capability

This document describes the reusable backend capability for accepting uploaded files before any domain-specific storage decision is made.

The capability is intentionally broader than problem data.
Implementation lives under `shared.application.upload`.

## Scope

The upload capability is responsible for:

- validating relative file paths
- preserving or normalizing text line endings according to policy
- expanding uploaded zip archives into validated relative paths
- returning prepared file entries that a domain-specific storage port can persist

The upload capability is not responsible for:

- authorization
- deciding which bucket or directory to use
- assigning domain ownership
- deciding which metadata must be persisted in a database

## Core Abstractions

- `StoredFilePath`
  A validated relative path using `/` separators.
- `FileUploadPolicy`
  Per-upload behavior flags such as line-ending normalization.
- `FileUploadPreparation`
  Converts raw uploaded bytes into prepared entries ready for storage.

## Path Rules

- paths must be relative
- paths must not start or end with `/`
- paths must not contain empty segments
- paths must not contain `.` or `..`
- path separators are normalized to `/`

## Text Normalization

Line-ending normalization must be opt-in.

Recommended behavior:

- default to preserving bytes exactly
- allow business flows to enable `CRLF -> LF` normalization for known text extensions
- never normalize bytes for files that are not confirmed text

This keeps upload handling reusable across:

- problem data
- source-code uploads
- markdown or configuration imports
- generic attachments

## Archive Expansion

Zip uploads are expanded before persistence.

Each entry must:

- map to a validated relative path
- optionally be placed under a validated target directory prefix
- respect the same text-normalization policy as direct file uploads

The storage layer receives flat prepared entries and does not need to know whether the source was a single file or an archive.
