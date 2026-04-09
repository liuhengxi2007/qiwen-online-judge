# Architecture Guardrails

The repository is organized around business domains first, with technical layers kept inside each domain.

## Frontend

- `src/features/auth`
  - Authentication, session state, registration, and user settings
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

### Shared Is Not a Junk Drawer

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

### Type Safety Rules

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

### Cross-Stack Naming Alignment

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

### Functional Core, Imperative Shell

Push side effects to the boundary.

Rules:

- keep parsing, validation, policy, and state transition logic pure when possible
- isolate IO, HTTP, database, time, randomness, and storage access at the edge
- domain functions should return data, decisions, or errors, not perform effects directly unless they are boundary services
- compose pure functions in the core, then execute effects in routers, clients, stores, or infrastructure adapters

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

### Hook Dependency Discipline

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

### Store Boundaries

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

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, models, and persistence for accounts and sessions
- `src/main/scala/domains/system/health`
  - Health endpoint and response model
- `src/main/scala/domains/shared`
  - Shared models used across domains, including pagination and lifecycle primitives
- `src/main/scala/database`
  - Shared database bootstrap and connection management

## Workers

- `judger`
  - Independent worker process for claiming judge tasks, running code, and reporting results

Worker processes are separate applications, not submodules of the backend runtime.

Rules:

- workers must not depend directly on the backend application project
- workers must not import backend `application`, `http`, `table`, or database code
- workers may call backend HTTP endpoints or other stable service boundaries
- if backend and a worker need shared Scala types, extract a small dedicated shared protocol module
- shared protocol modules may contain typed values, parsing, and codecs
- shared protocol modules must not contain SQL, routers, command handlers, or service orchestration

Prefer:

- `backend -> shared-judge-protocol`
- `judger -> shared-judge-protocol`

Avoid:

- `judger -> backend`
- sharing backend internals just to reuse one model file
- moving worker-specific execution code into backend `shared`

See also:

- `docs/contracts-scope.md`
- `docs/backend-contract-alignment.md`
- `docs/contract-checks.md`
- `docs/planner-patterns.md`
- `docs/resource-lifecycle-matrix.md`
- `docs/resource-access-policy-design.md`

### Backend Shared Rules

`domains/shared` should stay smaller than any real business domain.

Allow only:

- generic transport models
- lifecycle and pagination primitives
- small utility types with no business ownership

Do not move commands, policies, SQL, or domain workflows into `shared`.

## Next Domain Additions

Future OJ modules should follow the same layout:

- `domains/problemset`
- `domains/problem`
- `domains/submission`
- `domains/contest`
- `domains/usergroup`
- `domains/hack`

Each domain should own its:

- `application`
- `http`
- `model`
- `table`
