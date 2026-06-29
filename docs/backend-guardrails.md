# Backend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, objects, and persistence for accounts and sessions
- `src/main/scala/domains/user`
  - User profile, ranklist, settings, permission-management routes, and user-owned persistence/objects
- `src/main/scala/routes`
  - Top-level HTTP app/router composition that wires domain routers together
  - App-shell transport routes with no business-domain owner, such as the combined realtime SSE stream
- `src/main/scala/shared`
  - Dependency-pure shared payloads and platform helpers used across domains
  - `shared/objects`: shared transport/domain primitives such as pagination and lifecycle values
  - `shared/objects/transport`: shared HTTP transport payloads such as common success/error response bodies
  - `shared/objects/access`: shared access-control payload types only
  - `shared/application`: pure shared application helpers, such as generic access decisions and upload preparation
  - `shared/api`: shared API path/error helpers and small response helpers
- `src/main/scala/database`
  - Shared database bootstrap, connection management, and cross-domain persistence primitives that do not belong to one business domain
- `src/main/scala/domains/<domain>/utils`
  - Explicitly allowlisted owner-domain support code such as runtime adapters, event hubs, and storage/session/config helpers

## Backend File Templates

Backend domains should use consistent file-role templates instead of ad hoc mixed-responsibility files.

These templates define the default shape for a business domain, not a minimum file count.

For `utils`:

- domain `utils` is protected by `node scripts/check-structure-boundaries.mjs`; only files listed in `backendDomainUtilsAllowlist` may exist there
- keep only owner-domain support code that cannot live closer to `objects`, `table`, or `api`
- pure owner-domain rules may live in allowlisted `utils`; effectful orchestration belongs in the API object's `plan(...)`, while allowlisted `IO` or `Connection` utilities must stay limited to support adapters/helpers and must not own endpoint decisions
- before adding a new util file, prove it does not belong in `objects`, `table`, or `api`, then update both `backendDomainUtilsAllowlist` and this document in the same change
- keep allowed files directly under `utils/*.scala`; nested `utils` subdirectories are forbidden
- do not put API orchestration here; the API object's `plan(...)` remains the readable workflow
- do not create a domain `application` package for new code

For `routes`:

- `*Router.scala`
  thin aggregator that registers endpoint API objects in a list through the auth-owned API object router
- top-level `routes/*.scala` may host app-shell transport routes only when the route has no business-domain owner; realtime SSE belongs here, not under `domains/realtime`

For `api`:

- `api/<Name>.scala`
  one HTTP endpoint API object or dependency-carrying case class, including method, path, typed input decode, and the complete `plan(...)` orchestration body
- endpoint-specific input types should stay directly in the endpoint file
- multi-endpoint input tuples with no business meaning may live package-private in the owning `api` package

For `objects/request`:

- `<Name>.scala`
  typed command/query inputs decoded by HTTP endpoints and consumed by API plans, pure decisions, or table code, including inbound request bodies and query-derived list filters
- define the matching Circe encoder/decoder in the companion object when the type crosses the HTTP boundary

For `objects/response`:

- `<Name>.scala`
  read/output shapes returned by endpoint workflows, such as summaries, details, list responses, unread counts, upload results, and session/profile views
- define the matching Circe encoder/decoder in the companion object when the type crosses the HTTP boundary

Keep durable domain entities, value objects, enums, lifecycle types, slugs, ids, titles, and access policies directly under `objects/`. Put API payloads under `objects/request` and `objects/response`.
Put backend-only collaboration objects that are neither persistent entities nor HTTP payloads under `objects/internal`. These types are not frontend mirrors and do not participate in object alignment; expose them across domains only through an explicit domain-boundary allowlist entry when there is a real cross-domain workflow.
`objects/` files may define their own HTTP wire Circe encoders/decoders in companion objects. Persistence-only JSON codecs belong in `table/utils`, close to the table code that reads or writes that JSON column.
Bare `objects/` files must not import `objects/request` or `objects/response`; dependency direction is from request/response payloads toward core object types only.
`objects/request` and `objects/response` must not import `utils`, `routes`, or `table`.
`objects/` must not import `utils`, `routes`, or `table`; if an object transition needs data from a response shape, put that adapter in domain-owned support code outside `objects`.

Protocol modules are the exception: `judge-protocol` may keep Circe codecs next to protocol objects because those types are cross-process wire payloads rather than backend business models.

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
Do not move endpoint orchestration into `objects`, `utils`, or `table`. The API object's `plan(...)` is the readable workflow; pure business decisions belong in objects or allowlisted `utils`, and SQL belongs in table modules.

Current domain-owned utility allowlist:

- `domains/auth/utils/AuthAccountRules.scala`
- `domains/auth/utils/PasswordHasher.scala`
- `domains/auth/utils/AuthSessionCookies.scala`
- `domains/auth/utils/SessionCache.scala`
- `domains/auth/utils/SessionCacheConfig.scala`
- `domains/auth/utils/SessionConfig.scala`
- `domains/problem/utils/ProblemAccessRules.scala`
- `domains/problem/utils/ProblemDataUploadPreparation.scala`
- `domains/problemset/utils/ProblemSetAccessRules.scala`
- `domains/contest/utils/ContestAccessRules.scala`
- `domains/submission/utils/SubmissionAccessRules.scala`
- `domains/user/utils/UserApiRules.scala`
- `domains/user/utils/UserAvatarStorage.scala`
- `domains/user/utils/UserAvatarStorageConfig.scala`
- `domains/usergroup/utils/UserGroupAccessRules.scala`
- `domains/rating/utils/RatingCalculator.scala`

These files are domain-owned support code, not endpoint orchestration. Do not use `utils` as a replacement for API-object `plan(...)` bodies. Adding to this list is an explicit architecture decision, not the default place for miscellaneous helpers.

## Backend Domain Boundary Check

`node scripts/check-domain-boundaries.mjs` enforces cross-domain imports for backend domain code.

Allowed public domain boundaries:

- allowlisted durable/public object types under `domains/<domain>/objects`
- explicitly imported owner-domain API objects or backend-only API support files under `domains.<target>.api.<Name>` for cross-domain collaboration
- domain routers, such as `domains.problem.routes.ProblemRouter`
- auth-owned API-object protocol types in `domains.auth.api`: `AuthenticatedApi`, `AuthenticatedResponseApi`, `InternalOnlyApi`, `InternalOnlyAuthenticatedApi`, `PublicApi`, `PublicResponseApi`, `SiteManagerApi`, `ApiObjectContext`, `ApiObjectRouter`, and the `SessionResolver` object
- router/session wiring through `domains.auth.api.SessionStoreContext` values and `SessionStore` functions

Cross-domain application code should call the target domain's API object `plan(...)` instead of importing the target domain's `table` package. Wildcard imports such as `domains.problem.api.*` are intentionally forbidden across domains; name the API object being used.

Use `InternalOnlyApi` or `InternalOnlyAuthenticatedApi` for API-object plans that need a frontend-mirrored payload but must never be callable through HTTP. These API objects are registered normally, and their HTTP handler always returns `403 Forbidden` without decoding the request or running business logic.

The judge worker channel is the frontend API mirroring exception. Worker-only callable API objects in `domains.judge.api` and judge-registry worker endpoints in `domains.judger.api` do not need site frontend wrappers; plan-only owner-domain helpers still keep frontend payload mirrors even though they are not runtime-callable. Site management APIs such as judger listing still need frontend mirrors.

Current named API support surfaces:

- message event dispatch and payload typing through `domains.message.api`: `MessageEventHubContext`, `MessageEventHub`, and `MessageStreamEvent`
- notification event dispatch and payload typing through `domains.notification.api`: `NotificationEventHubContext`, `NotificationEventHub`, and `NotificationStreamEvent`
- judge registration/execution support through `domains.judge.api`: `JudgeConfig`, `JudgeTokenAuth`, and `JudgeTaskBuilder`
- problem data access through `domains.problem.api`: `ProblemDataStorageContext` values and `ProblemDataStorage` functions
- submission program access through `domains.submission.api`: `SubmissionProgramStorageContext` values and `SubmissionProgramStorage` functions
- submission judge state transitions used by the judge workflow through `domains.submission.api.SubmissionJudgeRules`
- cross-domain API-object collaborations explicitly named at the call site; new cross-domain table imports are forbidden

Current explicitly guarded non-API collaboration boundaries:

- contest access result payload: `EvaluateContestAccessResult`
- hack claimed-attempt payload: `ClaimedHackAttempt`
- notification persisted event payload typing: `NotificationKind` and `NotificationPayload`
- submission judge workflow model values: `ClaimedSubmission`, `SubmissionProgramManifest`, `SubmissionJudgeState`, `SubmissionStatus`, and `SubmissionVerdict`

## Functional Core, Imperative Shell

Backend code should keep business decisions pure and push effects to the boundary.

Rules:

- keep validation, permission decisions, state transitions, and draft-building logic pure where possible
- isolate JDBC, files, clocks, randomness, and network calls at the edge
- make effectful helpers explicit in their signatures, typically as `IO[...]`
- keep the API object's `plan(...)` as endpoint orchestration, with pure rule code in `objects` or allowlisted `utils`

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
`shared` must not import `domains.*`; pass domain actors, facts, or values in as type parameters or shared payloads instead.
Auth-aware session execution and API-object dispatch belong to `domains/auth/api`, while `shared/api` owns only domain-free transport helpers.

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
- keep endpoint orchestration in the API object's `plan(...)`, not in `utils`
- let API objects return typed outputs unless the endpoint needs a custom raw response
- keep auth/session resolution and generic dispatch in `domains/auth/api`
- keep `plan(...)` inputs at the HTTP boundary: raw session tokens and cookies must not enter business flow

Preferred structure for business CRUD domains:

- thin `*Router.scala`
  registers API objects with `ApiObjectRouter.routes(...)`
- `api/<Name>.scala`
  endpoint object/case class with method, path, decode, and `plan(...)`
- auth-owned API object router and `SessionResolver` object in `domains/auth/api`

Frontend and backend endpoint API basenames should match when both sides expose the endpoint. For example, the frontend `src/apis/problem/ListProblems.ts` client function corresponds to backend `domains/problem/api/ListProblems.scala`.

Do not add new `*HttpPlans.scala`, `*HttpPlanDefinitions.scala`, request mappers, response mappers, or command/query/mutation orchestration wrappers. Endpoint workflow belongs in the API object `plan(...)`.

Do not:

- reintroduce a generic `planner` domain
- replace REST paths with a global planner endpoint
- hide endpoint orchestration behind command/service wrappers
- move SQL into API objects

See also:

- [HTTP Planner Protocol](./http-planner-protocol.md)
- [Backend Wire Alignment](./backend-wire-alignment.md)
- [Wire Checks](./wire-checks.md)

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

- `api`
- `routes`
- `objects`
- `table`
- optional allowlisted `utils` modules when pure rules or owner-domain support code needs a clearer home than `objects`, `api`, or `table`

Exception:

- the internal `judge` domain may omit `objects` or `table` when it intentionally reuses protocol or persistence types owned elsewhere
- this exception must still keep endpoint orchestration in API object `plan(...)` bodies and its transport boundary in `api`/`routes`
