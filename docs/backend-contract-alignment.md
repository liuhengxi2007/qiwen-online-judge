# Backend Contract Alignment

This document defines how the backend should align with repository-level `contracts/` without collapsing backend domain types into transport types.

The goal is:

- shared HTTP contract shape
- backend-local domain model integrity
- explicit mapping at the boundary

The non-goal is:

- making backend domain entities identical to transport models
- moving backend business types into `contracts/`

## Rule

Backend code should treat `contracts/` as the transport contract source of truth.

That means:

- request and response field shape must match `contracts/`
- enum values exposed over HTTP must match `contracts/`
- backend domain objects may remain richer or stricter than contract objects

## Layer Split

Keep these roles separate:

- `contracts/`
  HTTP transport shape only
- backend `model/`
  backend domain values and HTTP-facing Scala models
- backend `application/`
  validation, policy, orchestration
- backend `http/`
  request decoding, response encoding, contract mapping

If a non-backend Scala process such as `judger` needs the same boundary types, do not make that process depend on the backend project directly.

Preferred split:

- `contracts/`
  TypeScript transport source of truth
- backend `model/`
  backend-owned HTTP-facing Scala models and domain values
- worker-local Scala models
  worker-owned execution and orchestration types
- optional shared Scala protocol module
  a small dependency used by both backend and workers for stable cross-process boundary types

Bad split:

- `judger -> backend`
- worker code importing backend routers, commands, or tables just to reuse a protocol case class

## Mapping Rule

Backend HTTP handlers should not expose raw domain entities directly.

Preferred flow:

1. decode HTTP request into backend request model
2. validate and convert into backend domain values
3. run application logic
4. map backend result into response model that matches `contracts/`
5. encode response

This implies:

- domain entities stay backend-owned
- transport models stay contract-shaped
- mapping happens at the HTTP boundary

## What Must Match `contracts/`

These backend HTTP-facing models must stay aligned with `contracts/`.

### Shared

- `ErrorResponse`
- pagination response shapes
- lifecycle enums exposed in JSON

### Auth

- `LoginRequest`
- `LoginResponse`
- `RegisterRequest`
- `SessionResponse`
- `UpdateOwnSettingsRequest`
- `UpdateManagedUserSettingsRequest`
- `UpdateUserPermissionsRequest`
- `AuthUserListItem`

### Problem

- `CreateProblemRequest`
- `UpdateProblemRequest`
- `ProblemListItem` / `ProblemDetail` JSON shape

### Problem Set

- `CreateProblemSetRequest`
- `UpdateProblemSetRequest`
- `LinkProblemRequest`
- `ProblemSetSummary`
- `ProblemSetDetail`
- `ProblemSetProblemSummary`

### User Group

- `CreateUserGroupRequest`
- `UpdateUserGroupRequest`
- `AddUserGroupMemberRequest`
- `UpdateUserGroupMemberRoleRequest`
- `UserGroupMember`
- `UserGroupSummary`
- `UserGroupDetail`
- `UserGroupRole`
- `AddUserGroupMemberRole`

## What Must Stay Backend-Local

Do not force these into `contracts/`.

- `AuthUser`
- `SiteManagerUser`
- `PasswordHash`
- `PlaintextPassword`
- `Username`
- command result enums
- policy results
- SQL row mapping structures
- persistence-specific helpers

Do not expose these as a shared Scala worker dependency either.

If a worker needs a type that currently lives beside backend-only code, split the shared boundary type into a separate protocol module instead of depending on the whole backend project.

## Alignment Check Checklist

Whenever a contract-shaped backend model changes, check all of the following:

1. `contracts/` still has the same field set and field names
2. JSON enum values still match exactly
3. optional vs nullable semantics still match exactly
4. frontend transport consumers still parse the same shape
5. backend HTTP encoders still produce contract-shaped JSON
6. backend HTTP decoders still accept contract-shaped JSON

## Required Review Discipline

When changing an HTTP-facing backend model:

- inspect the corresponding file in `contracts/`
- inspect the frontend API client using that contract
- inspect the backend HTTP mapper for that response

A backend PR that changes transport shape is incomplete if it changes only Scala models but not the matching `contracts/` file.

## Mapping Placement

Prefer contract alignment helpers in the backend `http/` layer, not in `shared` and not inside SQL/table code.

Good places:

- `domains/auth/http/*`
- `domains/problem/http/*`
- `domains/problemset/http/*`
- `domains/usergroup/http/*`

Bad places:

- `domains/shared/*` as a dumping ground
- `table/*` where persistence and transport get mixed
- domain entities themselves

## Preferred Naming

If a backend Scala model is HTTP-facing and contract-shaped, keep its name obviously aligned with the contract.

Examples:

- `LoginRequest`
- `SessionResponse`
- `ProblemSetDetail`

If a backend type is domain-internal, give it a name that signals domain ownership.

Examples:

- `AuthUser`
- `SiteManagerUser`
- `UpdateUserSettingsResult`

If a shared Scala protocol module is introduced, its names should still align with `contracts/` for the stable transport shape, while backend-only names should continue to signal backend ownership.

## Migration Strategy

Use this order when aligning existing backend code:

1. shared transport primitives
2. auth request/response models
3. problem request/response models
4. problemset request/response models
5. future domains such as contest, submission, usergroup, and hack

## Enforcement Direction

For now, enforcement is by review discipline and explicit boundary mapping.

Later, this can evolve into:

- generated schema checks
- contract snapshot tests
- response decoding tests against contract fixtures

Current lightweight check:

- `node scripts/check-contract-alignment.mjs`
