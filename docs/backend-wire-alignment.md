# Backend Wire Alignment

This document defines how backend HTTP-facing Scala objects stay aligned with frontend transport objects through mirrored files and alignment checks.

The goal is:

- one shared HTTP shape across frontend and backend
- backend-local domain object integrity
- explicit parsing and mapping at the backend HTTP boundary

The non-goal is:

- making backend domain entities identical to UI state
- forcing sensitive or internal backend types into a shared payload layer

## Rule

Frontend and backend boundary objects are the source of truth together.

That means:

- frontend `objects/request` and `objects/response` field shape must match backend `objects/request` and `objects/response`
- enum string values exposed over HTTP must match exactly
- backend domain objects may remain richer or stricter than HTTP payloads
- normal frontend API responses are trusted as mirrored payload types after JSON parsing
- if a backend object is mirrored by the frontend object layer, the mirrored type name and file basename must match exactly
- mirrored object files must stay focused on that mirrored type instead of accumulating mapping, persistence, or workflow responsibilities

## Layer Split

Keep these roles separate:

- frontend `objects/request` and `objects/response`
  frontend transport payload objects
- frontend `objects/<domain>/<Object>.ts`
  durable frontend object types plus parse/value helpers for forms, URLs, and local state
- frontend `apis/<domain>/<Name>.ts`
  endpoint message classes that declare HTTP method, path, request body, and response type
- backend `objects/request` and `objects/response`
  backend HTTP-facing command/query objects
- backend bare `objects`
  backend domain values and stable mirrored business concepts
- backend `objects/internal`
  backend-only collaboration objects that are not frontend mirrors and are skipped by object alignment
- backend `http`
  request mapping, response mapping, JSON codecs, and HTTP routing

If a non-backend Scala process such as `judger` needs the same boundary types, do not make that process depend on the backend project directly. Use a small protocol module for stable cross-process types.

## Mapping Rule

Backend HTTP handlers should not expose raw persistence rows or broad domain entities directly.

Preferred flow:

1. decode HTTP request into backend input object
2. validate and convert into backend domain values
3. run application logic
4. map backend result into backend output object
5. encode response consumed by the matching frontend response object

On the frontend, keep object-specific parsing and value extraction in the matching object file. API endpoint files should compose typed request objects and endpoint metadata directly; do not introduce frontend API codec layers.

## What Must Align

These surfaces are checked by `node scripts/check-object-alignment.mjs`:

- shared transport objects such as `ErrorResponse` and `SuccessResponse`, plus shared pagination shapes
- shared access and lifecycle values that are part of frontend/backend payloads
- domain `objects/request` files against backend `objects/request`
- domain `objects/response` files against backend `objects/response`
- domain `objects` files only when both sides expose the same key
- enum string values for exposed unions/enums
- backend `objects/internal` files are intentionally skipped

Do not force these into cross-stack alignment:

- auth secrets and session internals such as `PasswordHash`, `PlaintextPassword`, and `SessionToken`
- backend command result enums and policy decisions
- SQL row mapping structures and persistence helpers
- worker-only judge protocol tasks and requests

## Required Review Discipline

When changing an HTTP-facing object:

- inspect the matching frontend request/response file
- inspect the matching backend input/output file
- run `node scripts/check-object-alignment.mjs`

A change is incomplete if it updates only one side of the boundary.

## Alignment Checklist

1. Field names and field order match.
2. JSON enum values match exactly.
3. Optional vs nullable semantics are intentionally mapped.
4. Backend encoders and decoders still accept/produce the same shape.
5. Mirrored frontend and backend filenames still match exactly.
6. Mirrored type names still match exactly.
7. Mirrored object files have not accreted transport mapping, persistence helpers, reducers, or workflow logic.

## Enforcement

Current lightweight checks:

- `node scripts/check-object-alignment.mjs`
- `node scripts/check-api-alignment.mjs`

These checks do not replace runtime tests for behavior-bearing boundaries such as SSE, object storage, and HTTP message envelopes.
