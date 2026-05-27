# Qiwen Online Judge

Qiwen Online Judge is a type-safe online judge project with a React frontend, a Scala/http4s backend, PostgreSQL persistence, and an independent judge worker.

The project is organized by business domain rather than by technical layer. Cross-stack transport shapes are maintained by mirrored frontend/backend files and checked by scripts. Worker processes communicate with the backend through stable network protocols instead of importing backend internals.

## Features

- Account registration, login, logout, profile pages, and user settings.
- Site management for user records and permission flags.
- Problem management with Markdown statements, visibility/access control, time limit, space limit, and data file management.
- Problem set management with linked problems and access control.
- Submissions with globally increasing numeric ids, source-code detail pages, filtering, sorting, pagination, and visible judge results.
- A distributed judging direction: backend stores and exposes judge tasks, while `judger/` claims tasks, runs C++17 code in an isolate sandbox, and reports verdict plus runtime metrics.
- Blog support with public/private posts, general/problem blog types, per-user blog lists, per-problem blog areas, comments, replies, edit/delete, likes, and dislikes.
- User groups with membership and ownership management.
- Chinese and English UI copy for the current frontend pages.

## Repository Layout

- `frontend/`: Vite, React, TypeScript, Tailwind, shadcn/ui. Domain objects live under `frontend/src/objects/<domain>`, endpoint clients under `frontend/src/apis/<domain>`, and route code under `frontend/src/pages`.
- `backend/`: Scala 3, Cats Effect, http4s, Circe, PostgreSQL. Domain code lives under `backend/src/main/scala/domains/<domain>`.
- `judger/`: independent judge worker process.
- `judge-protocol-scala/`: shared Scala protocol module used across backend and worker boundaries.
- `docs/`: architecture, type-safety, contract-alignment, lifecycle, and worker guardrails.
- `scripts/`: maintenance checks such as contract alignment.
- `references/library-project/`: archived reference sample only; it is not current application source and is ignored by default ripgrep searches.

## Architecture Principles

- Keep ownership domain-first: `auth`, `problem`, `problemset`, `submission`, `blog`, `usergroup`, `judge`, and `judger` own their own backend objects/application/http/table code or frontend objects/apis/pages code.
- Keep mirrored object files type-only and name-aligned across frontend and backend.
- Parse raw JSON, route params, and form input at the boundary, then use named domain types such as `Username`, `ProblemSlug`, `SubmissionId`, and `BlogId`.
- Keep business decisions in pure domain/application code when possible; push database, HTTP, file, clock, and process effects to the edges.
- Keep workers independent from backend internals. Workers use HTTP/protocol boundaries, not backend application/table imports.

Start with [docs/architecture-guardrails.md](docs/architecture-guardrails.md) before making structural changes.

## Requirements

- Node.js and npm for the frontend and maintenance scripts.
- Java and sbt for the Scala backend and judger.
- PostgreSQL for backend persistence.
- WSL/Linux with `g++` and `isolate` for sandboxed local judging.

## Running Locally

Install frontend dependencies:

```bash
cd frontend
npm install
```

Start PostgreSQL and create the configured database/user. The backend defaults can be overridden with:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_MAX_POOL_SIZE`
- `DB_CONNECTION_TIMEOUT_MS`

Start the backend:

```bash
cd backend
sbt run
```

Start the frontend:

```bash
cd frontend
npm run dev
```

Start the judger in WSL when judging is needed:

```bash
cd judger
./run-wsl.sh
```

The frontend development server proxies API requests to the backend. If the frontend reports `ECONNREFUSED 127.0.0.1:8080`, start or restart the backend first.

## Validation Commands

Run these before committing related changes:

```bash
node scripts/check-all.mjs
npm --prefix frontend run typecheck
cd backend && sbt compile
```

`scripts/check-all.mjs` runs all `scripts/check-*.mjs` checks.
Run it whenever mirrored frontend/backend request, response, object, or shared type files change, whenever endpoint files under `src/apis` or backend `http/api` change, and after moving files across frontend or backend layers.

## Current Development Direction

The stable direction is to keep extending features by domain:

- New backend domains should use `objects`, `application`, `http`, and `table`.
- New frontend domains should use `objects`, `apis`, and `pages`, with runtime helpers in `system` only when ownership is not page or domain-specific.
- New cross-process judge behavior should go through `judge-protocol-scala` or stable HTTP protocol types, not backend internals.
- New durable resources should be added to [docs/resource-lifecycle-matrix.md](docs/resource-lifecycle-matrix.md).

Generated dependency/build output such as `node_modules/`, `dist/`, and `target/` should not be committed.
