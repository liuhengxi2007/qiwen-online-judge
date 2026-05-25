# Frontend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Frontend

- `src/features/auth`
  - Authentication, session state, and registration
- `src/features/user`
  - User profile, ranklist, and user-owned shared account models and API mappings
- `src/features/site-management`
  - Site-level user management and permission updates
- `src/features/dashboard`
  - Signed-in landing page and top-level entry points
- `src/shared`
  - Dependency-pure shared contracts, hooks, routing helpers, and platform primitives
- `src/shared/api`
  - Reusable HTTP request helpers and shared client error handling
- `src/shared/model/access`
  - Shared access-control contract types only
- `src/shared/domain`
  - Shared frontend domain primitives and pure helpers such as pagination, lifecycle values, and access parsers/codecs
- `src/shared/http`
  - Shared HTTP transport response types that mirror backend `shared/http`
- `src/components/ui`
  - Shared presentational UI primitives

## Frontend UI Copy and Color Rules

Frontend copy should describe user-facing actions and outcomes, not implementation details.

Rules:

- avoid developer-facing wording such as `domain`, `module`, `route`, `backend service`, `wired end-to-end`, or similar internal phrases in page copy
- prefer direct product language that explains what the user can do on the page
- avoid placeholder or process-era copy like `left panel`, `draft module`, or `this flow is still valid`
- use `judge system` or direct outcome language instead of internal worker names like `judger`

For colored action buttons with dark text:

- use `bg-*-300` as the default background color
- use `hover:bg-*-400` for hover state
- pair them with sufficiently dark text such as `text-*-950`
- keep one dominant color family per feature area unless there is a clear semantic reason to introduce another accent
- keep related actions in the same page or feature on the same color family whenever they represent the same kind of task

## Shared Is Not a Junk Drawer

`shared` exists only for code that is genuinely cross-domain and stable.

Put code in `shared` only if:

- it is already used by at least two domains, or
- it is a thin platform primitive that clearly belongs below business domains

Do not put code in `shared` when:

- it is only used by one domain right now
- it contains business rules for a specific domain
- it is a "maybe reusable later" abstraction
- it exists only to avoid choosing an owning domain

Preferred rule:

- keep code inside its domain first
- duplicate small code once if ownership is still clearer
- extract to `shared` only after the second real consumer appears

Good examples for `shared`:

- HTTP client primitives
- pagination types
- access contract types
- pure shared access parsers/codecs under `shared/domain/access`
- shared response payload types under `shared/model/response`
- generic routing helpers
- presentational UI primitives

Bad examples for `shared`:

- `shared/problemset-form`
- `shared/contest-policy`
- `shared/submission-status-mapper`
- `shared/model/access` parser or codec logic mixed into contract type files

`shared` must not import from `features/*`. Shared code sits below feature domains; feature-specific adapters should stay in the owning feature.

If ownership is ambiguous, default to the business domain that would be most damaged if the code changed.

## Type Safety Rules

Do not model domain objects with raw primitive types when a domain type exists.

Prefer:

- `Username` over `String`
- `ProblemId` over `Long`
- `ContestSlug` over `String`
- `SubmissionCode` over `String`

Rules:

- domain inputs and outputs should use domain types, not unlabelled primitives
- parsing and validation should happen at the boundary, then convert into typed values
- invalid states should be made unrepresentable where practical
- avoid "stringly typed" status, role, and visibility fields
- if a value has business meaning, give it a named type

Allowed use of primitives:

- transport and framework boundaries before parsing
- generic UI state with no domain meaning
- small local implementation details that do not cross module boundaries

Preferred pattern:

- raw request payload enters at the boundary
- boundary parser validates and converts to domain types
- application and business code only works with typed values

Bad examples:

- `def updateUser(username: String, role: String)`
- `type Problem = { id: number; status: string }`

Better examples:

- `def updateUser(username: Username, role: UserRole)`
- `type Problem = { id: ProblemId; accessPolicy: ResourceAccessPolicy }`

## Cross-Stack Naming Alignment

Cross-stack type names should align when they represent the same transport shape or the same stable business concept.

Rules:

- frontend `model/request` and `model/response` types must stay aligned with backend `model/request` and `model/response` boundary models
- if frontend and backend both expose the same response shape, use the same type name on both sides
- do not introduce backend-only aliases like `SummaryView`, `ListItem`, or `MemberRecord` when they mean the same thing as an existing contract-facing type
- do not introduce frontend-only aliases for the same contract shape unless the frontend model has meaningfully different fields or semantics
- if a type is only an internal application or persistence model, it may differ, but its name must reflect that different role explicitly

Prefer:

- `ProblemSummary` on both sides for the problem list item shape
- `ProblemDetail` on both sides for the problem detail shape
- `UserGroupMember` on both sides for the member payload shape

Avoid:

- `ProblemListItem` when the shared shape is already `ProblemSummary`
- `ProblemSetSummaryView` when the shared shape is already `ProblemSetSummary`
- `UserGroupMemberRecord` when the shared shape is already `UserGroupMember`

When the shape is identical, prefer one name everywhere.
When the shape is different, prefer different names that make the difference obvious.

## Frontend API Files

Feature API clients should be split one endpoint per file.

Rules:

- endpoint clients live in `src/features/<domain>/http/api/<Name>.ts`
- each endpoint file exports the idiomatic function name used by hooks and pages, for example `ListProblems.ts` exports `listProblems`
- do not add domain-level `http/api/*-client.ts` barrels
- do not keep compatibility barrels under `src/features/<domain>/api`
- non-JSON boundary helpers, such as download URL builders or realtime event URL helpers, also live in matched API files
- when a frontend endpoint has a backend route, the frontend and backend API basenames must match exactly, with only the extension differing
- run `node scripts/check-api-alignment.mjs` when endpoint files change

Hooks and pages should import directly from the matched endpoint file.

## Frontend Layer Boundaries

Feature layer roles:

- `model`
  mirrored domain values and stable business concepts; one top-level type per file
- `model/request`
  feature API request payload models; one top-level type per file
- `model/response`
  feature API response payload models; one top-level type per file
- `lib`
  pure feature helpers such as parsers, display formatting, validation, permission helpers, and draft builders
- `state`
  pure reducer state, actions, and transitions for pages and editors
- `http`
  endpoint clients and feature boundary codecs
- `hooks`
  React orchestration for queries, mutations, reducers, browser state, and side effects
- `components`
  feature-owned presentational and interaction UI
- `pages`
  route-level page composition

Rules:

- a long route page may keep page-private components or pure helpers under `pages/<PageName>/components` or `pages/<PageName>/functions`; promote them to feature-level `components`, `hooks`, `lib`, or `state` only after another page actually uses them
- `src/features/<domain>/domain` no longer exists; do not add feature domain barrels or compatibility re-exports
- bare `src/features/<domain>/model` must not import `model/request`, `model/response`, `http`, `lib`, `state`, `hooks`, `components`, or `pages`
- `src/features/<domain>/model/request` and `src/features/<domain>/model/response` must not import `http`, `state`, `hooks`, `components`, or `pages`
- `src/features/<domain>/lib` and `src/features/<domain>/state` must not import `hooks`, `components`, or `pages`
- React hooks belong in `hooks`, even when they expose display preferences or other domain-adjacent UI state
- import model, request, response, lib, and state symbols directly from the owning file instead of through barrels
- run `node scripts/check-structure-boundaries.mjs` after moving files across frontend layers

If a bare model needs a type that currently lives in `model/response`, either move the shared concept into bare `model` or introduce a core model type and let the response layer reuse it.

## Frontend and Backend Type System Mirror Rule

Frontend and backend cross-stack type layers must stay fully aligned for shared transport and domain types.

This is stricter than "close enough naming".

Rules:

- if frontend and backend represent the same stable type, the type name must match exactly
- if frontend and backend represent the same stable type, the file basename must match exactly
- do not mix mirrored type definitions with unrelated orchestration, reducers, HTTP clients, or UI state
- lightweight per-type boundary support may stay local to the mirrored file when it remains tightly coupled to that type
  examples: codec instances, enum string conversion, branded-type helpers, tiny parse helpers
- mapping between transport, domain, and UI state should still live outside mirrored type files
- validation that combines multiple fields or encodes business workflow should still live outside mirrored type files
- do not keep one large aggregate model file on one side while splitting the same mirrored types into many files on the other side
- do not introduce frontend-only or backend-only aliases for a mirrored type
- keep core domain models directly under bare `model/`; use backend and frontend `model/request` for inbound/request-shaped inputs and `model/response` for outbound read shapes

Required path rules:

- backend request payload:
  `backend/src/main/scala/domains/<domain>/model/request/<Name>.scala`
- frontend request payload:
  `frontend/src/features/<domain>/model/request/<Name>.ts`

- backend response payload:
  `backend/src/main/scala/domains/<domain>/model/response/<Name>.scala`
- frontend response payload:
  `frontend/src/features/<domain>/model/response/<Name>.ts`

- backend domain model:
  `backend/src/main/scala/domains/<domain>/model/<Name>.scala`
- frontend domain model:
  `frontend/src/features/<domain>/model/<Name>.ts`

- backend shared model:
  `backend/src/main/scala/shared/model/<Name>.scala`
- frontend shared model:
  `frontend/src/shared/model/<Name>.ts`

- backend shared access:
  `backend/src/main/scala/shared/model/access/<Name>.scala`
- frontend shared access:
  `frontend/src/shared/model/access/<Name>.ts`

- backend shared response:
  `backend/src/main/scala/shared/model/response/<Name>.scala`
- frontend shared response:
  `frontend/src/shared/model/response/<Name>.ts`

The mapping must be simple enough that a script can derive one side from the other without special cases.

Prefer:

- `ProblemSummary.scala` and `ProblemSummary.ts`
- `ProblemSetProblemSummary.scala` and `ProblemSetProblemSummary.ts`
- `AccessPolicy.scala` and `AccessPolicy.ts`

Avoid:

- `Problem.scala` on one side and `ProblemTypes.ts` on the other
- one `model/` file defining multiple top-level model types
- one side using `ProblemListItem` while the other uses `ProblemSummary`
- one mirrored type file quietly becoming a feature-level dumping ground for unrelated helpers

See also:

- [Backend Contract Alignment](./backend-contract-alignment.md)
- [Contract Checks](./contract-checks.md)

## Functional Core, Imperative Shell

Push side effects to the boundary.

Rules:

- keep parsing, validation, policy, and state transition logic pure when possible
- isolate IO, HTTP, database, time, randomness, and storage access at the edge
- pure core functions should return data, decisions, or errors, not perform effects directly unless they are boundary services
- compose pure functions in the core, then execute effects in routers, clients, stores, or infrastructure adapters
- if a helper performs JDBC, clock, randomness, file, or network work, its signature must expose that effect explicitly, typically as `IO[...]`

Preferred layering:

- boundary layer: HTTP handlers, database adapters, browser storage, fetch, clocks
- application layer: orchestration of use cases
- pure feature core: rules, typed data, state transitions

Good examples:

- validate a `CreateProblemSetRequest` into typed input before touching the database
- compute permission decisions with pure policy functions
- build next submission status from current status with a pure transition function

Bad examples:

- route handlers mixing JSON parsing, permission rules, SQL, and response formatting in one block
- stores or hooks hiding domain rules together with network side effects
- domain objects depending directly on framework APIs

When forced to choose, prefer:

- one more small pure function
- one more explicit typed value

over:

- one more shared mutable state path
- one more untyped string flag
- one more helper that mixes business logic and IO

## Hook Dependency Discipline

React hook dependency arrays must be precise.

Rules:

- include values that are actually read inside the hook body
- do not depend on whole objects when only a few fields or methods are used
- do not depend on `state` wholesale when only selected fields are read
- do not add dependencies just to silence uncertainty; refactor to stable values first
- if a dependency changes every render because of object identity, extract the specific stable function or field that is actually needed

Prefer:

- `mutation.submitSettings` over `mutation`
- `state.displayName` and `state.email` over `state`
- scalar flags over wrapper option objects

Avoid:

- callbacks that depend on entire reducer state objects
- effects that depend on freshly created wrapper objects
- broad dependency arrays that cause needless recomputation or callback churn

## Frontend Feature File Templates

Frontend feature modules should use one consistent split strategy instead of ad hoc large files.

For `model`:

- one mirrored cross-stack type per file
- type definition files stay focused on the mirrored type itself
  lightweight per-type helpers are allowed, but reducers, API clients, and feature orchestration do not belong here

For `lib`:

- `<feature>-parsers.ts`
  parsing and value accessors
- `<feature>-form.ts`
  validation of create or update drafts
- `<feature>-*-support.ts`
  pure helpers for building drafts, permissions, or derived decisions shared by hooks
- `<feature>-display.ts`
  pure display formatting helpers

For `state`:

- `<feature>-*-state.ts`
  reducer state, actions, and pure transitions for page or editor state

For `hooks`:

- `use-*-query.ts`
  one remote read flow
- `use-*-action.ts` or `use-*-mutation.ts`
  one remote write flow
- `use-*-editor-state.ts`
  thin wrapper around a reducer from `state/*-state.ts`
- `use-*-page-model.ts`
  page orchestration only; compose queries, actions, and reducers, but keep pure helpers in `lib` and reducers in `state`

For `pages`:

- page files should primarily compose extracted components and page models
- repeated or large visual sections should move into `components/`

For `shared/i18n`:

- `messages.ts`
  locale registry only
- `messages/<locale>/<domain>.ts`
  one locale/domain message group per file
- avoid one monolithic locale file once copy spans multiple business domains

These templates exist to keep state transitions, contract mapping, and UI composition separate.

## Store Boundaries

State stores are allowed only for real shared state containers. They must not become hidden query or workflow layers.

Rules:

- `src/stores` is reserved for app-shell state that is not owned by a single business domain
- `features/*/stores` should be rare and only used for domain-local shared state across multiple pages or entry points
- stores may hold state, derived flags, and simple state transitions
- stores must not directly own fetch, mutation, permission redirect, or orchestration logic
- query loading belongs in hooks
- mutation side effects belong in hooks or explicit clients
- page models may coordinate query and mutation results, but should not hide that coordination inside a store

Prefer:

- a `use...Query` hook that loads data and exposes `replace...` helpers for local synchronization
- a `use...Mutation` hook that performs one request and returns a typed result
- a store only for session state, theme state, or other cross-page state that is truly shared

Avoid:

- stores that call APIs directly and also manage redirect decisions
- stores that mix cache state, loading state, permission errors, and mutation workflows
- adding a feature store only to avoid choosing between a hook and a reducer
