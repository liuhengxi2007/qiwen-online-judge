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
  - Cross-feature hooks and routing helpers
- `src/shared/api`
  - Reusable HTTP request helpers and shared client error handling
- `src/shared/domain`
  - Shared frontend domain primitives such as pagination and lifecycle types
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
- generic routing helpers
- presentational UI primitives

Bad examples for `shared`:

- `shared/problemset-form`
- `shared/contest-policy`
- `shared/submission-status-mapper`

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
- application/domain code only works with typed values

Bad examples:

- `def updateUser(username: String, role: String)`
- `type Problem = { id: number; status: string }`

Better examples:

- `def updateUser(username: Username, role: UserRole)`
- `type Problem = { id: ProblemId; accessPolicy: ResourceAccessPolicy }`

## Cross-Stack Naming Alignment

Cross-stack type names should align when they represent the same transport shape or the same stable business concept.

Rules:

- HTTP contract types in `contracts/` are the naming source of truth for both frontend and backend boundary models
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
- the domain `http/api/*-client.ts` file is the only frontend API barrel for a domain
- do not keep compatibility barrels under `src/features/<domain>/api`
- non-JSON boundary helpers, such as download URL builders or realtime event URL helpers, also live in matched API files
- when a frontend endpoint has a backend route, the frontend and backend API basenames must match exactly, with only the extension differing
- run `node scripts/check-api-alignment.mjs` when endpoint files change

Avoid adding new endpoint implementations directly to aggregate `*-client.ts` files.

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
- do not put API-only payload DTOs in `model/`; use the domain HTTP request folder for inbound backend payloads, backend `application/view` for outbound read shapes, and frontend HTTP response folders for received payload types

Required path rules:

- backend HTTP request payload:
  `backend/src/main/scala/domains/<domain>/http/request/<Name>.scala`
- frontend HTTP request payload:
  `frontend/src/features/<domain>/http/request/<Name>.ts`

- backend application view / response shape:
  `backend/src/main/scala/domains/<domain>/application/view/<Name>.scala`
- frontend HTTP response payload:
  `frontend/src/features/<domain>/http/response/<Name>.ts`

- backend domain model:
  `backend/src/main/scala/domains/<domain>/model/<Name>.scala`
- frontend domain model:
  `frontend/src/features/<domain>/model/<Name>.ts`

- backend shared model:
  `backend/src/main/scala/domains/shared/model/<Name>.scala`
- frontend shared model:
  `frontend/src/shared/model/<Name>.ts`

- backend shared access:
  `backend/src/main/scala/domains/shared/access/<Name>.scala`
- frontend shared access:
  `frontend/src/shared/access/<Name>.ts`

The mapping must be simple enough that a script can derive one side from the other without special cases.

Prefer:

- `ProblemSummary.scala` and `ProblemSummary.ts`
- `SubmissionLifecycle.scala` and `SubmissionLifecycle.ts`
- `AccessPolicy.scala` and `AccessPolicy.ts`

Avoid:

- `Problem.scala` on one side and `ProblemTypes.ts` on the other
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
- domain functions should return data, decisions, or errors, not perform effects directly unless they are boundary services
- compose pure functions in the core, then execute effects in routers, clients, stores, or infrastructure adapters
- if a helper performs JDBC, clock, randomness, file, or network work, its signature must expose that effect explicitly, typically as `IO[...]`

Preferred layering:

- boundary layer: HTTP handlers, database adapters, browser storage, fetch, clocks
- application layer: orchestration of use cases
- domain layer: pure rules, typed data, state transitions

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

For `domain`:

- `<feature>.ts`
  facade-only file that re-exports the domain surface
- `<feature>-parsers.ts`
  parsing and value accessors
- `<feature>-contract.ts`
  contract-to-domain and domain-to-contract mapping
- `<feature>-form.ts`
  validation of create or update drafts
- `<feature>-*-state.ts`
  reducer state, actions, and pure transitions for page or editor state
- `<feature>-*-support.ts`
  pure helpers for building drafts, permissions, or derived decisions shared by hooks

For `hooks`:

- `use-*-query.ts`
  one remote read flow
- `use-*-action.ts` or `use-*-mutation.ts`
  one remote write flow
- `use-*-editor-state.ts`
  thin wrapper around a reducer from `domain/*-state.ts`
- `use-*-page-model.ts`
  page orchestration only; compose queries, actions, and reducers, but keep pure helpers in `domain`

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
