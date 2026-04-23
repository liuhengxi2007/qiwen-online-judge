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
- if a backend model is mirrored by the frontend model layer, the mirrored type name must match exactly
- if a backend model is mirrored by the frontend model layer, the mirrored file basename must match exactly
- mirrored model files on both sides must stay focused on that mirrored type instead of accumulating mapping, persistence, or workflow responsibilities

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
- `RegisterResponse`
- `SessionResponse`
- `UpdateOwnSettingsRequest`
- `UpdateManagedUserSettingsRequest`
- `UpdateUserPermissionsRequest`
- `AuthUserListItem`
- `UserProfileResponse`

### Blog

- `CreateBlogRequest`
- `UpdateBlogRequest`
- `VoteBlogRequest`
- `CreateBlogCommentRequest`
- `UpdateBlogCommentRequest`
- `VoteBlogCommentRequest`
- `BlogCommentSummary`
- `BlogSummary`
- `BlogDetail`
- `BlogType`
- `BlogVisibility`
- `BlogVote`

### Judger

- `RegisteredJudgerListItem`

### Problem

- `CreateProblemRequest`
- `UpdateProblemRequest`
- `ProblemSummary`
- `ProblemDetail`

### Problem Set

- `CreateProblemSetRequest`
- `UpdateProblemSetRequest`
- `AddProblemToProblemSetRequest`
- `ProblemSetSummary`
- `ProblemSetDetail`
- `ProblemSetProblemSummary`

### Submission

- `CreateSubmissionRequest`
- `SubmissionSummary`
- `SubmissionDetail`
- `SubmissionLanguage`
- `SubmissionStatus`
- `SubmissionVerdict`

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

## Required Frontend Mirror

When a backend model participates in the shared frontend-backend type system, it must have a directly corresponding frontend model file.

Required mappings:

- `backend/src/main/scala/domains/<domain>/model/<Name>.scala`
  mirrors
  `frontend/src/features/<domain>/model/<Name>.ts`

- `backend/src/main/scala/domains/shared/model/<Name>.scala`
  mirrors
  `frontend/src/shared/model/<Name>.ts`

- `backend/src/main/scala/domains/shared/access/<Name>.scala`
  mirrors
  `frontend/src/shared/access/<Name>.ts`
  only when that access type is part of the shared frontend-backend contract surface

Rules:

- do not merge multiple mirrored types into one file on one side only
- do not split one mirrored type into multiple files on one side only
- do not rename a mirrored type on one side without renaming the corresponding file and type on the other side
- keep transport-to-domain mapping, multi-field validation, persistence support, and workflow behavior out of mirrored model files
- lightweight codec or parse support that belongs only to that one mirrored type is allowed when it does not blur layer ownership

## Alignment Check Checklist

Whenever a contract-shaped backend model changes, check all of the following:

1. `contracts/` still has the same field set and field names
2. JSON enum values still match exactly
3. optional vs nullable semantics still match exactly
4. frontend transport consumers still parse the same shape
5. backend HTTP encoders still produce contract-shaped JSON
6. backend HTTP decoders still accept contract-shaped JSON
7. mirrored frontend and backend filenames still match exactly
8. mirrored frontend and backend type names still match exactly
9. mirrored model files have not accreted transport mapping, persistence helpers, reducers, or workflow logic

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
3. blog request/response models
4. problem request/response models
5. problemset request/response models
6. submission request/response models
7. usergroup request/response models
8. future domains such as contest and hack

## Enforcement Direction

For now, enforcement is by review discipline and explicit boundary mapping.

Later, this can evolve into:

- generated schema checks
- contract snapshot tests
- response decoding tests against contract fixtures

Current lightweight check:

- `node scripts/check-contract-alignment.mjs`
