# Backend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, models, and persistence for accounts and sessions
- `src/main/scala/domains/system/health`
  - Health endpoint and response model
- `src/main/scala/domains/shared`
  - Shared models used across domains, including pagination and lifecycle primitives
- `src/main/scala/database`
  - Shared database bootstrap and connection management

## Backend File Templates

Backend domains should use consistent file-role templates instead of ad hoc mixed-responsibility files.

For `application`:

- `*CommandResults.scala`
  result ADTs returned by the application layer
- `*CommandSupport.scala`
  internal helpers shared by multiple commands in the domain
- `*QueryCommands.scala`
  read-only use cases
- `*MutationCommands.scala`
  write-oriented use cases
- `*Commands.scala`
  facade-only file that re-exports the domain command surface

Domain-specific specialized command files are allowed when a domain has one extra responsibility that does not fit cleanly into generic query or mutation buckets.

For `http`:

- `*Router.scala`
  path matching and route dispatch only
- `*HttpHandlers.scala`
  request decoding, auth/session wrapping, command invocation
- `*HttpResponses.scala`
  translation from command results to HTTP responses
- `*HttpSupport.scala`
  optional HTTP-only shared helpers for the domain

For `table`:

- `*Table.scala`
  outward-facing persistence API
- `*TableSchema.scala`
  DDL, migration, initialization
- `*TableSql.scala`
  SQL constants
- `*TableSupport.scala`
  row readers, bind helpers, and persistence-local support code

These templates exist to stop large files from mixing routing, orchestration, SQL, migrations, and decoding in one place.

## Functional Core, Imperative Shell

Backend code should keep business decisions pure and push effects to the boundary.

Rules:

- keep validation, permission decisions, state transitions, and draft-building logic pure where possible
- isolate JDBC, files, clocks, randomness, and network calls at the edge
- make effectful helpers explicit in their signatures, typically as `IO[...]`
- keep `http` as the execution shell, `application` as orchestration, and pure rule code in `model` or dedicated support modules

Prefer:

- pure access-decision helpers that consume already-fetched facts
- pure lifecycle transitions such as submission judge-state updates
- `table` methods for persistence and row mapping only

Avoid:

- hiding SQL or file IO inside a helper that looks pure
- mixing response formatting, permission logic, and JDBC in one block
- moving effectful orchestration into mirrored model files

## Backend Shared Rules

`domains/shared` should stay smaller than any real business domain.

Allow only:

- generic transport models
- lifecycle and pagination primitives
- small utility types with no business ownership

Do not move commands, policies, SQL, or domain workflows into `shared`.

## Backend HTTP Planner Protocol

The backend uses a per-domain HTTP planner protocol, not a global planner domain.

Rules:

- keep normal REST routers such as `ProblemRouter` and `UserGroupRouter`
- model each HTTP use case as a typed plan inside the owning domain
- keep planners in the `http` layer, not in `application`
- let plans return typed outputs, then map those outputs to `Response[IO]`
- choose transaction boundaries explicitly with plain vs transaction plan variants

Preferred structure for business CRUD domains:

- `*HttpPlans.scala`
  endpoint-level plan implementations
- `*HttpPlanDefinitions.scala`
  registered plans plus `toResponse` mapping
- shared executor in `domains/shared/http`
- thin `*Router.scala`
  route matching plus executor call

`auth/http` is the exception. It keeps its own richer protocol because it needs public, authenticated, and site-manager plan families.

Do not:

- reintroduce a generic `planner` domain
- replace REST paths with a global planner endpoint
- move domain rules, SQL, or policy evaluation into plans

See also:

- [HTTP Planner Protocol](./http-planner-protocol.md)
- [Backend Contract Alignment](./backend-contract-alignment.md)
- [Contract Checks](./contract-checks.md)

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
