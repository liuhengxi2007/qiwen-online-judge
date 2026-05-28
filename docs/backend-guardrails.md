# Backend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, objects, and persistence for accounts and sessions
- `src/main/scala/domains/user`
  - User profile, ranklist, settings, permission-management routes, and user-owned persistence/objects
- `src/main/scala/server/http`
  - Top-level HTTP app/router composition that wires domain routers together
- `src/main/scala/server/health`
  - Health endpoint and response object
- `src/main/scala/shared`
  - Dependency-pure shared contracts and platform helpers used across domains
  - `shared/objects`: shared transport/domain primitives such as pagination and lifecycle values
  - `shared/objects/access`: shared access-control contract types only
  - `shared/application`: pure shared application helpers, such as generic access decisions and upload preparation
  - `shared/http`: shared HTTP transport objects, codecs, API paths, and small response/error helpers
- `src/main/scala/database`
  - Shared database bootstrap, connection management, and cross-domain persistence primitives that do not belong to one business domain

## Backend File Templates

Backend domains should use consistent file-role templates instead of ad hoc mixed-responsibility files.

These templates define the default shape for a business domain, not a minimum file count.

For `application`:

- do not add new endpoint orchestration here
- keep only reusable infrastructure adapters or pure helpers that cannot live closer to `objects`, `rules`, `table`, storage, or events
- migrate legacy command/query/mutation files into API-object `plan(...)` bodies or smaller rule/adapter modules when touching those endpoints

For `http`:

- `*Router.scala`
  thin aggregator that registers endpoint API objects in a list through the auth-owned API object router
- `api/<Name>.scala`
  one HTTP endpoint API object or dependency-carrying case class, including method, path, typed input decode, and the complete `plan(...)` orchestration body
- `codec/*HttpCodecs.scala`
  Circe encoders/decoders for HTTP request and response wire formats
- `codec/*ModelHttpCodecs.scala`
  Circe encoders/decoders for durable object values when those values appear in HTTP request or response wire formats
- `utils/*HttpSupport.scala`
  optional HTTP-only shared helpers for the domain

For `objects/request`:

- `<Name>.scala`
  typed command/query inputs decoded by HTTP endpoints and consumed by application/table code, including inbound request bodies and query-derived list filters
- do not define Circe encoders/decoders here; HTTP wire codecs belong in the owning domain's `http/codec`

For `objects/response`:

- `<Name>.scala`
  read/output shapes returned by application use cases, such as summaries, details, list responses, unread counts, upload results, and session/profile views
- do not define Circe encoders/decoders here; HTTP wire codecs belong in the owning domain's `http/codec`

Keep durable domain entities, value objects, enums, lifecycle types, slugs, ids, titles, and access policies directly under `objects/`. Put API contract payloads under `objects/request` and `objects/response`.
Put backend-only collaboration objects that are neither persistent entities nor HTTP payloads under `objects/internal`. These types are not frontend mirrors and do not participate in HTTP contract alignment; expose them across domains only through an explicit domain-boundary allowlist entry when there is a real cross-domain workflow.
`objects/` files must not import Circe or define JSON encoders/decoders. HTTP JSON codecs belong in the owning domain's `http/codec`, or in `shared/http/codec` for shared transport primitives. Persistence-only JSON codecs belong in `table/utils`, close to the table code that reads or writes that JSON column.
Bare `objects/` files must not import `objects/request` or `objects/response`; dependency direction is from request/response payloads toward core object types only.
`objects/request` and `objects/response` must not import `application`, `http`, or `table`.
`objects/` must not import `application`, `http`, or `table`; if an object transition needs data from a response shape, put that adapter in `application` support code.

Protocol modules are the exception: `judge-protocol-scala` may keep Circe codecs next to protocol objects because those types are cross-process wire contracts rather than backend business models.

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
Do not move endpoint orchestration into `objects`, `application`, or `table`. The API object's `plan(...)` is the readable workflow; pure business decisions belong in rules/helpers and SQL belongs in table modules.

Current application-adapter exceptions:

- `domains/problem/application/LocalProblemDataStorage.scala`
- `domains/problem/application/MinioProblemDataStorage.scala`
- `domains/problem/application/ProblemDataStorage.scala`
- `domains/problem/application/ProblemDataStorageConfig.scala`
- `domains/problem/application/ProblemDataUploadPreparation.scala`
- `domains/auth/application/*Session*.scala`
- `domains/auth/application/PasswordHasher.scala`
- `domains/message/application/MessageEventHub.scala`
- `domains/notification/application/NotificationEventHub.scala`
- `domains/notification/application/NotificationStreamEvent.scala`
- `domains/judge/application/JudgeConfig.scala`
- `domains/judge/application/JudgeTaskBuilder.scala`

These files are allowed to stay in `application` for now because they implement
domain-owned adapter behavior and moving them to a new infrastructure layer would
be a separate behavior-risking migration. Do not use this exception as a pattern
for new session, event, judge, or storage files without documenting the boundary decision first.

## Functional Core, Imperative Shell

Backend code should keep business decisions pure and push effects to the boundary.

Rules:

- keep validation, permission decisions, state transitions, and draft-building logic pure where possible
- isolate JDBC, files, clocks, randomness, and network calls at the edge
- make effectful helpers explicit in their signatures, typically as `IO[...]`
- keep the API object's `plan(...)` as endpoint orchestration, with pure rule code in `objects`, `rules`, or dedicated support modules

Prefer:

- pure access-decision helpers that consume already-fetched facts
- pure lifecycle transitions such as submission judge-state updates
- `table` methods for persistence and row mapping only

Avoid:

- hiding SQL or file IO inside a helper that looks pure
- mixing response formatting, permission logic, and JDBC in one block
- moving effectful orchestration into mirrored object files
- using a small domain as an excuse to accumulate unrelated application concerns in one file

## Backend Shared Rules

`shared` should stay smaller than any real business domain.

Allow only:

- generic transport objects
- lifecycle and pagination primitives
- small utility types with no business ownership
- cross-domain platform primitives whose ownership is genuinely shared
  examples: API path/error helpers, reusable access-control primitives used by multiple resource domains

Cross-domain persistence helpers belong in `database` when they are infrastructure-level and do not have a single business-domain owner.
`shared` must not import `domains.*`; pass domain actors, facts, or values in as type parameters or shared contracts instead.
Auth-aware session execution and API-object dispatch belong to `domains/auth/http`, while `shared/http` owns only domain-free transport helpers.

Do not move these into `shared`:

- business-domain-specific commands or workflows
- policies that belong clearly to one business domain
- feature-specific SQL added only to avoid choosing an owning domain
- large mixed-responsibility utility modules

If code in `shared` starts reading like a hidden `problem`, `problemset`, or `usergroup` subdomain, or like a JDBC/DDL utility layer, it is in the wrong place.

## Backend HTTP API Object Protocol

The backend uses a per-domain HTTP API object protocol, not a global planner domain.

Rules:

- keep normal REST routers such as `ProblemRouter` and `UserGroupRouter`
- model each HTTP use case as a typed API object inside the owning domain
- keep endpoint orchestration in the API object's `plan(...)`, not in `application`
- let API objects return typed outputs unless the endpoint needs a custom raw response
- keep auth/session resolution and generic dispatch in `domains/auth/http`
- keep `plan(...)` inputs at the HTTP boundary: raw session tokens and cookies must not enter business flow

Preferred structure for business CRUD domains:

- thin `*Router.scala`
  registers API objects with `ApiObjectRouter.routes(...)`
- `http/api/<Name>.scala`
  endpoint object/case class with method, path, decode, and `plan(...)`
- auth-owned API object router and `SessionResolver` in `domains/auth/http`

Frontend and backend endpoint API basenames should match when both sides expose the endpoint. For example, the frontend `src/apis/problem/ListProblems.ts` client function corresponds to backend `http/api/ListProblems.scala`.

Do not add new `*HttpPlans.scala`, `*HttpPlanDefinitions.scala`, request mappers, response mappers, or command/query/mutation orchestration wrappers. Endpoint workflow belongs in the API object `plan(...)`.

Do not:

- reintroduce a generic `planner` domain
- replace REST paths with a global planner endpoint
- hide endpoint orchestration behind command/service wrappers
- move SQL into API objects

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

- `http`
- `objects`
- `table`
- optional `rules`, `storage`, or `events` modules when pure rules or adapters need a clearer home than `http` or `table`

Exception:

- the internal `judge` domain may omit `objects` or `table` when it intentionally reuses protocol or persistence types owned elsewhere
- this exception must still keep endpoint orchestration in API object `plan(...)` bodies and its transport boundary in `http`
