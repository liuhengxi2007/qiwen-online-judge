# Contract Scope

This document defines which types should become shared frontend-backend API contracts, and which types must remain local to either the frontend or backend.

The goal is not to unify every type in the repository.

The goal is to give HTTP boundary models a single source of truth while preserving domain-specific representations inside each runtime.

## Rule

Put a type in `contract` when:

- it is sent over HTTP between frontend and backend
- both sides must agree on its field shape
- both sides must agree on its enum values

Keep a type local when:

- it exists only for UI state or interaction flow
- it exists only for backend domain logic or persistence
- it contains runtime-specific invariants that do not belong in transport models

## Shared Contract Types

These types should become shared contracts.

### Shared

- `ErrorResponse`
- `PageResponse<T>`
- `ResourceVisibility`

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

### Problem

- `CreateProblemRequest`
- `UpdateProblemRequest`
- `ProblemSummary`
- `ProblemDetail`

### Problem Set

- `CreateProblemSetRequest`
- `UpdateProblemSetRequest`
- `LinkProblemRequest`
- `ProblemSetProblemSummary`
- `ProblemSetSummary`
- `ProblemSetDetail`

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

## Frontend-Only Types

These types must remain local to the frontend.

- page state objects
- reducer state objects
- hook state objects
- form draft objects
- local validation result objects
- local navigation or interaction-only helpers

Examples:

- `LoginPageState`
- `RegisterPageState`
- `UserSettingsDraft`
- `CreateProblemPageState`
- `CreateProblemSetPageState`

## Backend-Only Types

These types must remain local to the backend.

- domain entities
- value objects with server-only invariants
- command and policy result types
- database row models
- persistence helpers

Examples:

- `AuthUser`
- `SiteManagerUser`
- `Username`
- `PlaintextPassword`
- `PasswordHash`
- `AuthUserCommands.UpdateUserSettingsResult`
- `Problem`
- `ProblemSet`

## Similar Names, Different Roles

Some names may look similar across layers. They should not be collapsed into one type if they serve different roles.

- `SessionResponse`
  Shared contract
- `AuthSession`
  Frontend-local authenticated session state
- `AuthUser`
  Backend-local domain entity

- `ProblemDetail`
  Shared contract
- `Problem`
  Backend-local domain entity
- problem editor state
  Frontend-local editing state

- `ProblemSetDetail`
  Shared contract
- `ProblemSet`
  Backend-local domain entity

## Migration Order

Prefer this extraction order:

1. shared contract primitives
2. auth HTTP models
3. problem HTTP models
4. problemset HTTP models
5. future modules such as contest, submission, usergroup, and hack

## Non-Goal

Do not use `contract` as a way to force frontend and backend internal domain models into the same representation.

The intended split is:

- contract models are shared
- frontend domain models stay frontend-specific
- backend domain models stay backend-specific
