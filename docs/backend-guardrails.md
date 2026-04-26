# Backend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, models, and persistence for accounts and sessions
- `src/main/scala/domains/user`
  - User profile, ranklist, settings, permission-management routes, and user-owned persistence/models
- `src/main/scala/domains/system/health`
  - Health endpoint and response model
- `src/main/scala/domains/shared`
  - Shared models used across domains, including pagination and lifecycle primitives
- `src/main/scala/database`
  - Shared database bootstrap, connection management, and cross-domain persistence primitives that do not belong to one business domain

## Backend File Templates

Backend domains should use consistent file-role templates instead of ad hoc mixed-responsibility files.

These templates define the default shape for a business domain, not a minimum file count.

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

Domains may omit any application file role that they do not need yet.
For example, a small or integration-heavy domain may have only `*Commands.scala`, or only a query/mutation split, as long as responsibilities stay clear.
When a responsibility becomes non-trivial, split it into its matching role file instead of continuing to grow an unrelated file.

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

The rule is "you may have fewer files, but you may not blur responsibilities".
Do not move query/mutation orchestration into `model`, HTTP decoding into `application`, or business decisions into `table`.

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
- using a small domain as an excuse to accumulate unrelated application concerns in one file

## Backend Shared Rules

`domains/shared` should stay smaller than any real business domain.

Allow only:

- generic transport models
- lifecycle and pagination primitives
- small utility types with no business ownership
- cross-domain platform primitives whose ownership is genuinely shared
  examples: shared HTTP execution support, reusable access-control primitives used by multiple resource domains

Cross-domain persistence helpers belong in `database` when they are infrastructure-level and do not have a single business-domain owner.

Do not move these into `shared`:

- business-domain-specific commands or workflows
- policies that belong clearly to one business domain
- feature-specific SQL added only to avoid choosing an owning domain
- large mixed-responsibility utility modules

If code in `shared` starts reading like a hidden `problem`, `problemset`, or `usergroup` subdomain, or like a JDBC/DDL utility layer, it is in the wrong place.

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

Exception:

- the internal `judge` domain may omit `model` or `table` when it intentionally reuses protocol or persistence types owned elsewhere
- this exception must still keep its orchestration in `application` and its transport boundary in `http`
