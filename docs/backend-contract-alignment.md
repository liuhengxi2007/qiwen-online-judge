# Backend Contract Alignment

This document defines how backend HTTP-facing Scala models stay aligned with frontend transport models through mirrored files and alignment checks.

The goal is:

- one shared HTTP shape across frontend and backend
- backend-local domain model integrity
- explicit parsing and mapping at the frontend/backend boundary

The non-goal is:

- making backend domain entities identical to UI state
- forcing sensitive or internal backend types into a shared contract layer

## Rule

Frontend and backend boundary models are the source of truth together.

That means:

- frontend `http/request` and `http/response` field shape must match backend `application/input` and `application/output`
- enum string values exposed over HTTP must match exactly
- backend domain objects may remain richer or stricter than HTTP payloads
- frontend adapters may keep local wire DTO aliases when they parse raw JSON into branded frontend types
- if a backend model is mirrored by the frontend model layer, the mirrored type name and file basename must match exactly
- mirrored model files must stay focused on that mirrored type instead of accumulating mapping, persistence, or workflow responsibilities

## Layer Split

Keep these roles separate:

- frontend `http/request` and `http/response`
  frontend transport payload models
- frontend `domain/*-contract.ts`
  boundary parsing and serialization helpers
- backend `application/input` and `application/output`
  backend HTTP-facing command/query models
- backend `model`
  backend domain values and stable mirrored business concepts
- backend `http`
  request decoding, response encoding, and HTTP routing

If a non-backend Scala process such as `judger` needs the same boundary types, do not make that process depend on the backend project directly. Use a small protocol module for stable cross-process types.

## Mapping Rule

Backend HTTP handlers should not expose raw persistence rows or broad domain entities directly.

Preferred flow:

1. decode HTTP request into backend input model
2. validate and convert into backend domain values
3. run application logic
4. map backend result into backend output model
5. encode response consumed by the matching frontend response model

On the frontend, parse raw response payloads at the feature boundary before handing values to UI state.

## What Must Align

These surfaces are checked by `node scripts/check-contract-alignment.mjs`:

- shared response models such as `ErrorResponse`, `SuccessResponse`, and pagination shapes
- shared access and lifecycle values that are part of frontend/backend payloads
- feature `http/request` files against backend `application/input`
- feature `http/response` files against backend `application/output`
- feature `model` files only when both sides expose the same key
- enum string values for exposed unions/enums

Do not force these into cross-stack alignment:

- auth secrets and session internals such as `PasswordHash`, `PlaintextPassword`, and `SessionToken`
- backend command result enums and policy decisions
- SQL row mapping structures and persistence helpers
- worker-only judge protocol tasks and requests

## Required Review Discipline

When changing an HTTP-facing model:

- inspect the matching frontend request/response file
- inspect the matching backend input/output file
- inspect the frontend adapter that parses or serializes the payload
- run `node scripts/check-contract-alignment.mjs`

A change is incomplete if it updates only one side of the boundary.

## Alignment Checklist

1. Field names and field order match.
2. JSON enum values match exactly.
3. Optional vs nullable semantics are intentionally mapped.
4. Frontend adapters still parse the raw shape.
5. Backend encoders and decoders still accept/produce the same shape.
6. Mirrored frontend and backend filenames still match exactly.
7. Mirrored type names still match exactly.
8. Mirrored model files have not accreted transport mapping, persistence helpers, reducers, or workflow logic.

## Enforcement

Current lightweight checks:

- `node scripts/check-contract-alignment.mjs`
- `node scripts/check-api-alignment.mjs`

These checks do not replace runtime tests or endpoint-level response decoding tests.
