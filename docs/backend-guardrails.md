# Backend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, models, and persistence for accounts and sessions
- `src/main/scala/domains/user`
  - User profile, ranklist, settings, permission-management routes, and user-owned persistence/models
- `src/main/scala/server/http`
  - Top-level HTTP app/router composition that wires domain routers together
- `src/main/scala/server/health`
  - Health endpoint and response model
- `src/main/scala/shared`
  - Shared models used across domains, including pagination and lifecycle primitives
- `src/main/scala/database`
  - Shared database bootstrap, connection management, and cross-domain persistence primitives that do not belong to one business domain

## Backend File Templates

Backend domains should use consistent file-role templates instead of ad hoc mixed-responsibility files.

These templates define the default shape for a business domain, not a minimum file count.

For `application`:

- `*CommandResults.scala`
  result ADTs returned by the application layer
- `utils/*CommandSupport.scala`
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
  thin aggregator that combines endpoint API route files
- `api/<Name>.scala`
  one HTTP endpoint route fragment, including path matching, input parsing/decoding, plan execution, and response mapping for that endpoint
- `*HttpHandlers.scala`
  request decoding, auth/session wrapping, command invocation
- `response/*HttpResponses.scala`
  translation from command results to HTTP responses
- `codec/*HttpCodecs.scala`
  Circe encoders/decoders for HTTP request and response wire formats
- `codec/*ModelHttpCodecs.scala`
  Circe encoders/decoders for durable model values when those values appear in HTTP request or response wire formats
- `utils/*HttpSupport.scala`
  optional HTTP-only shared helpers for the domain

For `application/input`:

- `<Name>.scala`
  typed command/query inputs decoded by HTTP endpoints and consumed by application/table code, including inbound request bodies and query-derived list filters
- do not define Circe encoders/decoders here; HTTP wire codecs belong in the owning domain's `http/codec`

For `application/output`:

- `<Name>.scala`
  read/output shapes returned by application use cases, such as summaries, details, list responses, unread counts, upload results, and session/profile views
- do not define Circe encoders/decoders here; HTTP wire codecs belong in the owning domain's `http/codec`

Do not put API-only payload DTOs in `model/`. Keep durable domain entities, value objects, enums, lifecycle types, slugs, ids, titles, and access policies in `model/`.
`model/` files must not import Circe or define JSON encoders/decoders. HTTP JSON codecs belong in the owning domain's `http/codec`, or in `shared/http/codec` for shared transport primitives. Persistence-only JSON codecs belong in `table/utils`, close to the table code that reads or writes that JSON column.
Non-HTTP layers must not import from `http.request` or `http.response`; use `application/input` for inputs and `application/output` for read/output shapes that application, table, or model code needs to name.
`model/` must not import `application`, `http`, or `table`; if a model transition needs data from an output shape, put that adapter in `application` support code.

Protocol modules are the exception: `judge-protocol-scala` may keep Circe codecs next to protocol models because those types are cross-process wire contracts rather than backend business models.

For `table`:

- `table/<table_name>/`
  one folder per persisted table or closely owned table group; use `snake_case` for multi-word folder names and keep the Scala package aligned with the directory
- `*Table.scala`
  outward-facing persistence API and operation SQL placed immediately before the method that uses it
- `*TableSchema.scala`
  DDL, migration, and initialization SQL
- optional same-folder support files
  row readers, bind helpers, codecs, and package-local persistence support for that table

These templates exist to stop large files from mixing routing, orchestration, SQL, migrations, and decoding in one place.

Do not create `*TableSql.scala` files or group operation SQL into one constants block at the top of a table. Prefer:

```scala
private val insertBookSQL = ...
def insertBook(...) = ...

private val deleteBookSQL = ...
def deleteBook(...) = ...
```

Run `node scripts/check-table-sql-locality.mjs` after table-layer SQL edits.

The rule is "you may have fewer files, but you may not blur responsibilities".
Do not move query/mutation orchestration into `model`, HTTP wire decoding into `application`, or business decisions into `table`.

Current application-adapter exceptions:

- `domains/message/application/JdbcMessageRepository.scala`
- `domains/problem/application/LocalProblemDataStorage.scala`
- `domains/problem/application/MinioProblemDataStorage.scala`

These files are allowed to stay in `application` for now because they implement
domain-owned adapter behavior and moving them to a new infrastructure layer would
be a separate behavior-risking migration. Do not use this exception as a pattern
for new JDBC or storage files without documenting the boundary decision first.

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

`shared` should stay smaller than any real business domain.

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
- shared executor in `shared/http`
- thin `*Router.scala`
  aggregator of `http/api/<Name>.scala` endpoint files

Frontend and backend endpoint API basenames should match when both sides expose the endpoint. For example, the frontend `http/api/ListProblems.ts` client function corresponds to backend `http/api/ListProblems.scala`.

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

- `domains/blog`
- `domains/message`
- `domains/notification`
- `domains/problem`
- `domains/problemset`
- `domains/submission`
- `domains/user`
- `domains/usergroup`
- `domains/contest`
- `domains/hack`

Each domain should own its:

- `application`
- `http`
- `model`
- `table`

Exception:

- the internal `judge` domain may omit `model` or `table` when it intentionally reuses protocol or persistence types owned elsewhere
- this exception must still keep its orchestration in `application` and its transport boundary in `http`
