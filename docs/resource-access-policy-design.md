# Resource Access Policy Design

This document proposes a replacement for the current `private | group | public` visibility model used by problems and problem sets.

The replacement is a shared access-control abstraction that:

- supports public resources
- supports owner-only resources
- supports visibility through one or more user groups
- supports direct grants to individual users
- avoids duplicating ACL logic separately inside the `problem` and `problemset` domains

## Why Replace `private | group | public`

The current visibility enum is too small for the real access rules the system needs.

Problems with the current model:

- `group` is underspecified
  - it does not say which group or how many groups
- direct user grants do not fit the enum
- the enum is copied into multiple domains instead of modeling access control once
- the UI shape encourages a single selector even when access is actually relationship-based
- future expansion would likely create more ad hoc flags instead of one coherent model

The target model should express access as policy plus grants, not as a small status-like enum.

## Design Goals

- keep one shared ACL abstraction for all protected resource domains
- make group grants and user grants first-class and structurally identical
- keep ownership and admin override rules explicit
- allow gradual migration from the current enum
- keep side effects at persistence and HTTP boundaries, not inside policy code

## Core Model

Each protected resource has an access policy.

```ts
type BaseAccess = 'owner_only' | 'public'

type AccessSubject =
  | { kind: 'user'; username: string }
  | { kind: 'user_group'; slug: string }

type ResourceAccessPolicy = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubject[]
}
```

### Meaning

- `baseAccess = owner_only`
  - only the owner can view the resource by default
- `baseAccess = public`
  - every authenticated viewer can view the resource by default
- `viewerGrants`
  - extra viewers that may access the resource in addition to the default policy

This makes direct user sharing a normal case instead of a special exception path.

## Shared Abstraction

The access-control mechanism should live in a shared backend module, not inside `problem` or `problemset`.

Recommended shared concepts:

- `ResourceKind`
- `BaseAccess`
- `AccessSubject`
- `ResourceAccessPolicy`
- `ResourceViewerGrant`
- `AccessPolicyEvaluator`

Recommended backend location:

- `backend/src/main/scala/domains/shared/access`

This shared module should only own generic ACL behavior.

It should not own:

- problem-specific validation
- problem-set-specific HTTP contracts
- ownership semantics for each domain
- domain-specific list item or detail response models

Each business domain still owns its resource model and use cases. The shared module only owns how grants are represented and evaluated.

## Access Evaluation Rules

The evaluator should answer: may actor `A` view resource `R` with policy `P`?

The rule order should be:

1. owner can view
2. global override actors can view
   - for example site managers, or problem managers where product rules allow it
3. if `baseAccess = public`, allow view
4. if actor matches a direct `user` grant, allow view
5. if actor is a member of any granted `user_group`, allow view
6. otherwise deny

The same rule set should be reused in:

- detail access
- list filtering
- any future count or search endpoints

## Mapping From Current Visibility

The current enum maps naturally into the new model.

| Current Value | New Policy |
| --- | --- |
| `private` | `baseAccess = owner_only`, `viewerGrants = []` |
| `group` | `baseAccess = owner_only`, `viewerGrants = [{ kind: 'user_group', slug: ... }]` |
| `public` | `baseAccess = public`, `viewerGrants = []` |

The important change is that the new model does not limit the resource to a single group grant.

## Why User Grants Should Reuse The Same Subject Model

Direct user authorization should not be implemented as a special side table or a separate exception rule.

Instead, a direct user grant should be one more `AccessSubject`.

Good:

```ts
viewerGrants: [
  { kind: 'user_group', slug: 'round-123-testers' },
  { kind: 'user', username: 'alice' },
]
```

Bad:

- `visibility = group` plus a separate `sharedUsers` field
- `visibility = private` plus one-off exception code for named users
- separate policy branches for group-sharing and user-sharing

Using one subject model keeps:

- the API simpler
- the UI simpler
- the SQL schema simpler
- future expansion easier

## Persistence Model

Keep the default visibility on the main resource record and store grants separately.

Recommended main-table field:

- `base_access`

Recommended values:

- `owner_only`
- `public`

Recommended shared grant table:

```sql
create table resource_viewer_grants (
  resource_kind varchar(32) not null,
  resource_id uuid not null,
  subject_kind varchar(32) not null check (subject_kind in ('user', 'user_group')),
  subject_key varchar(64) not null,
  created_at timestamptz not null,
  primary key (resource_kind, resource_id, subject_kind, subject_key)
);
```

### Field Meaning

- `resource_kind`
  - identifies the domain resource type such as `problem` or `problem_set`
- `resource_id`
  - the resource primary key
- `subject_kind`
  - `user` or `user_group`
- `subject_key`
  - canonical lowercase username for `user`
  - canonical lowercase group slug for `user_group`

This shared table avoids creating one nearly identical grant table per domain.

## Contract Shape

The transport contract should stop exposing the old `visibility` enum for resources that move to ACL-based access control.

Recommended transport shape:

```ts
export type BaseAccess = 'owner_only' | 'public'

export type AccessSubject =
  | { kind: 'user'; username: string }
  | { kind: 'user_group'; slug: string }

export type ResourceAccessPolicy = {
  baseAccess: BaseAccess
  viewerGrants: AccessSubject[]
}
```

Then:

- `ProblemDetail` includes `accessPolicy`
- `ProblemSetDetail` includes `accessPolicy`
- create and update requests also use `accessPolicy`

If list responses need to stay smaller, they may expose a compact summary instead of the full grant list, but the detail response should expose the actual policy.

## Query Strategy

Do not fetch all resources and filter them in application code.

Prefer SQL that can answer visibility directly:

- owner check
- `base_access = 'public'`
- direct user grant exists
- granted user group membership exists

This likely means:

- joining `resource_viewer_grants`
- joining `user_group_memberships` for group subjects
- or using `exists` subqueries where that keeps SQL clearer

The policy function should remain pure. The table layer should perform the actual joins and existence checks.

## Frontend Model

The frontend should stop presenting `Private / Group / Public` as a single selector.

Recommended UI direction:

- a `Public` toggle for `baseAccess`
- a `Visible To` section for grants
- add grant by user group
- add grant by user
- remove grant actions from the same list

This better reflects the real model:

- public is a baseline property
- sharing is a list of subjects

## Ownership And Editing Rules

Access to view a resource and access to edit its ACL are separate concerns.

Recommended default:

- ACL edit rights should follow the domain's management policy
- global override actors may edit ACL only if the existing product rules already allow them to manage the resource

In this repository's current `problem` and `problemset` domains:

- the resource stores a `creator` identity for attribution and baseline access evaluation
- that `creator` field is not itself a management permission
- create, update, delete, and ACL edits remain governed by manager permissions unless product rules change

This document does not propose changing ownership semantics. It only replaces the visibility model.

## Migration Plan

Prefer a staged migration.

### Stage 1

- introduce shared ACL types in backend and contracts
- add `base_access` and `resource_viewer_grants`
- continue reading old `visibility`
- map old values into the new policy internally

### Stage 2

- backfill existing data
- `private` becomes `owner_only` with no grants
- `public` becomes `public` with no grants
- current `group` records become `owner_only` plus their explicit group grants

### Stage 3

- switch frontend create and edit pages to the new policy editor
- expose actual grants in detail APIs

### Stage 4

- remove the old `visibility` enum from contracts and persistence
- delete compatibility mapping code

## Implementation Checklist

This section turns the design into an execution plan for this repository.

### Contracts

Add shared ACL transport types in:

- `contracts/shared.ts`

Planned additions:

- `BaseAccess`
- `AccessSubject`
- `ResourceAccessPolicy`

Planned removals after migration:

- `ResourceVisibility`

Update resource contracts in:

- `contracts/problem.ts`
- `contracts/problemset.ts`

Required changes:

- replace `visibility` with `accessPolicy` on detail responses
- replace `visibility` with `accessPolicy` on create requests
- replace `visibility` with `accessPolicy` on update requests

Suggested list-response rule:

- keep list responses lightweight
- either expose `baseAccess` only, or expose a compact `accessSummary`
- do not send full grant lists in wide list pages unless product needs it

### Backend Shared Module

Create a shared ACL module under:

- `backend/src/main/scala/domains/shared/access`

Planned types:

- `ResourceKind`
- `BaseAccess`
- `AccessSubject`
- `ResourceAccessPolicy`
- `ResourceViewerGrant`

Planned responsibilities:

- parse and validate ACL transport input
- represent canonical subject keys
- evaluate viewer access with pure logic

Do not put domain-specific ownership logic in this module.

### Backend Persistence

Add a shared grant table and shared table helper.

Recommended database change:

```sql
create table resource_viewer_grants (
  resource_kind varchar(32) not null,
  resource_id uuid not null,
  subject_kind varchar(32) not null check (subject_kind in ('user', 'user_group')),
  subject_key varchar(64) not null,
  created_at timestamptz not null,
  primary key (resource_kind, resource_id, subject_kind, subject_key)
);
```

Add a shared table adapter under:

- `backend/src/main/scala/domains/shared/access/ResourceViewerGrantTable.scala`

Required operations:

- replace all grants for one resource
- list grants for one resource
- test whether a user has direct access
- test whether a user has access through any granted user group

### Backend Problem Domain

Update these areas:

- `backend/src/main/scala/domains/problem/model`
- `backend/src/main/scala/domains/problem/application`
- `backend/src/main/scala/domains/problem/http`
- `backend/src/main/scala/domains/problem/table`

Required work:

- replace `visibility` in problem transport-facing models with `accessPolicy`
- add `base_access` persistence to problems
- load and save grants through the shared ACL table
- update list queries to filter by:
  - owner
  - global override
  - public base access
  - direct user grant
  - granted user-group membership
- update detail queries or command logic to enforce the same visibility rule

Important rule:

- the problem domain still owns problem creation, update, and ownership
- it delegates grant storage and grant evaluation to the shared ACL module

### Backend Problem Set Domain

Update these areas:

- `backend/src/main/scala/domains/problemset/model`
- `backend/src/main/scala/domains/problemset/application`
- `backend/src/main/scala/domains/problemset/http`
- `backend/src/main/scala/domains/problemset/table`

Required work mirrors the problem domain:

- replace `visibility` with `accessPolicy`
- add `base_access` persistence
- store and load grants through the shared ACL table
- filter list and detail visibility with the shared ACL rule

### Frontend Shared Model

Update:

- `frontend/src/shared/domain`

Required work:

- replace shared `ResourceVisibility` usage with shared ACL types
- keep transport-facing ACL types aligned with `contracts/shared.ts`

### Frontend Problem And Problem Set UI

Update:

- create pages
- detail pages
- editor state
- form validation
- API client request builders

Required UI change:

- remove the single `Private / Group / Public` selector
- add a `Public` switch for `baseAccess`
- add a `Visible To` grant list
- allow adding:
  - a user group by slug
  - a user by username
- allow removing any existing grant

Recommended editing behavior:

- edit ACL in the same resource form
- preserve explicit order in the UI only for presentation
- treat grants as a set in the backend

### Validation Rules

All ACL inputs should be validated at the boundary.

Required rules:

- usernames must already be canonical lowercase usernames
- user-group slugs must already be canonical lowercase slugs
- duplicate grants are rejected or deduplicated before persistence
- grants pointing to missing users or missing groups are rejected
- owner identity is not stored as a viewer grant

### Compatibility Phase

During migration, keep the old model readable but not authoritative.

Recommended temporary approach:

- continue reading old `visibility`
- derive internal `ResourceAccessPolicy` from it
- write new resources using the new ACL shape
- do not let new frontend code depend on old `visibility`

This phase should be short-lived and explicitly temporary.

### Suggested Delivery Order

1. add shared contract types
2. add backend shared ACL module
3. add persistence support for grants
4. switch backend problem and problem-set create, update, detail, and list flows
5. switch frontend problem and problem-set forms and detail pages
6. backfill old records
7. remove old `visibility` transport and persistence paths

### Done Criteria

The migration is complete only when all of the following are true:

- `contracts/*` no longer expose `ResourceVisibility` for problems or problem sets
- backend list and detail access both use the shared ACL rule
- frontend create and edit flows submit `accessPolicy`
- direct user grants and user-group grants both work end to end
- old `private | group | public` persistence is removed
- there is one shared ACL implementation rather than duplicated domain-specific versions

## Non-Goals

This design does not attempt to define:

- edit permissions separate from view permissions for non-owners
- nested groups
- time-bounded grants
- organization-level or contest-level ACL subjects

Those can be added later if needed, but they should extend the same shared ACL model instead of replacing it.

## Recommendation

Implement ACL as a shared cross-resource mechanism.

Do not:

- keep extending `private | group | public`
- create one-off direct-user exceptions
- duplicate near-identical ACL tables for each resource type

Do:

- keep `baseAccess` minimal
- treat user and user-group sharing as the same kind of grant
- centralize ACL evaluation in a shared module
- let each domain own only its resource-specific rules and data
