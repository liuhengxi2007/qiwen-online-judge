# Qiwen Online Judge Backend

This is the Scala 3 + Cats Effect + http4s backend for Qiwen Online Judge.

## Commands

```bash
sbt run
sbt compile
```

The service listens on `http://0.0.0.0:8080` by default.

## Structure

- `src/main/scala/Main.scala`
  Service startup and router wiring.
- `src/main/scala/database`
  Shared database configuration, connection/session management, and
  cross-domain persistence primitives.
- `src/main/scala/domains/<domain>`
  Domain-first backend code. Most business domains are split into `objects`,
  `http`, and `table`, with optional `rules` and guarded `utils`.
- `src/main/scala/shared`
  Cross-domain primitives such as pagination, access control, shared HTTP
  execution support, and generic response objects.

Current domains are `auth`, `blog`, `judge`, `judger`, `message`, `notification`,
`problem`, `problemset`, `submission`, `system`, `user`, and `usergroup`.

## Layer Rules

- `objects`
  Durable domain entities, value objects, enums, lifecycle types, slugs, ids, and
  titles. Object files must not import `utils`, `http`, or `table`.
- `objects/request`
  Typed command/query inputs decoded at HTTP boundaries and consumed by API plans, rules, or table code.
- `objects/response`
  Read/output shapes returned by endpoint workflows.
- `rules`
  Optional pure validation, permission, lifecycle, and draft-building helpers.
- `http`
  Endpoint routing, request decoding, plan execution, and response mapping.
- `table`
  PostgreSQL persistence APIs, SQL, schema setup, and row mapping.
- `utils`
  Guarded owner-domain infrastructure such as storage/session/config helpers or
  event hubs. New files must update `backendDomainUtilsAllowlist` in
  `scripts/check-structure-boundaries.mjs` and the guardrails docs.

Detailed rules live in [docs/backend-guardrails.md](../docs/backend-guardrails.md).

## Database Configuration

Defaults:

- `DB_HOST=127.0.0.1`
- `DB_PORT=5432`
- `DB_NAME=qiwen_online_judge`
- `DB_USER=db`
- `DB_PASSWORD=root`

Optional overrides:

- `DB_MAX_POOL_SIZE`
- `DB_CONNECTION_TIMEOUT_MS`

`DatabaseSession` owns transaction connection lifecycle. Application commands
should use `DatabaseSession.withTransactionConnection` rather than managing
connections directly.

## Validation

From the repository root, run the relevant checks before committing:

```bash
npm --prefix frontend run typecheck
node scripts/check-object-alignment.mjs
node scripts/check-api-alignment.mjs
node scripts/check-structure-boundaries.mjs
cd backend
sbt compile
```

Run the object alignment check when mirrored object/request/response types change. Run the API
alignment check when endpoint files under `http/api` change. Run the structure
boundary check after moving files across backend layers.
