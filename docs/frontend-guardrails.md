# Frontend Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Frontend Layout

Top-level source folders:

- `src/apis`
  Endpoint clients and HTTP boundary codecs, grouped by domain.
- `src/objects`
  Frontend business objects, request/response payload types, parsers, pure display helpers, and pure form helpers.
- `src/objects/shared`
  Shared business objects and pure cross-domain helpers such as pagination and resource-access primitives.
- `src/pages`
  Route pages, page-owned components, page hooks, route policies, URL/search-param state, page collaboration stores, and display-only state.
- `src/router.tsx`
  Top-level browser router composition and root layout imported by `src/main.tsx`.
- `src/pages/stores`
  Cross-page page-layer collaboration stores such as session, realtime inbox, notification counters, and internal storage adapters.
- `src/pages/objects`
  Page-layer-only pure orchestration objects. These may only be imported by files under `src/pages`.
- `src/system`
  App runtime helpers such as the HTTP client and i18n runtime/messages.
- `src/components/ui`
  Generic presentational UI primitives.

Current business domains include `auth`, `blog`, `dashboard`, `judger`, `message`,
`notification`, `problem`, `problemset`, `site-management`, `submission`, `user`,
and `usergroup`.

## Dependency Direction

Allowed direction:

- `objects/shared`
  Dependency-pure shared objects. Do not import app layers or domain objects.
- `objects/<domain>`
  May import `objects/shared` and other `objects/<domain>` packages only for real business type relationships. Never import `pages`, `apis`, `system`, or `components/ui`.
- `system`
  May import `objects/shared` only. Do not import domain objects, APIs, pages, or UI components.
- `apis/<domain>`
  May import `objects`, `objects/shared`, peer API codecs, and `system/api`. Never import `pages`.
- `components/ui`
  May import external UI libraries and `system/i18n` for generic translated labels. Never import pages or domain objects.
- `pages/stores`
  May import frontend objects, peer page-store modules, and external runtime libraries. Stores must not import APIs, system helpers, UI components, page hooks/components/objects, or concrete pages.
- `pages`
  May import APIs, objects, page stores, system helpers, UI components, page components, page hooks, and page objects.

Hard rule: `src/pages/objects/**` is for page-layer-only pure objects. It is for
route orchestration concepts such as navigation intent, URL/search-param state,
and page display state value objects. Put API payloads and domain contracts in
`src/objects/shared` or `src/objects/<domain>`.

Run `node scripts/check-structure-boundaries.mjs` after moving files across frontend layers.

## API Files

Endpoint clients live in `src/apis/<domain>/<Name>.ts`.

Rules:

- one endpoint client per file
- the frontend endpoint basename must match the backend `http/api/<Name>.scala` basename when both sides expose the endpoint
- HTTP boundary codecs live in `src/apis/<domain>/codecs/*HttpCodecs.ts`
- do not add compatibility barrels; import endpoint and codec files directly
- non-JSON helpers such as download URL builders or realtime event URL helpers live in the matched endpoint file
- run `node scripts/check-api-alignment.mjs` when endpoint files change

## Objects

Use `src/objects/<domain>` for durable frontend business concepts:

- mirrored domain values and stable business concepts
- `request` payloads
- `response` payloads
- parsers and value accessors
- pure display formatting helpers
- pure form validation and draft builders

Use `src/objects/shared` only after there is a real shared owner. Good examples:

- pagination types and helpers
- shared response payloads
- access-control contracts and parsers/codecs
- resource lifecycle helpers

Do not put domain-specific policies, forms, or status mappers in shared objects
just to avoid choosing an owner.

## Pages

Route pages live at `src/pages/<PageName>/index.tsx`.

Page-private code belongs under the page directory:

- `components`
- `hooks`
- `functions`
- other page-local support folders when needed

Reusable page-layer code belongs under:

- `src/pages/components` for independent shared page UI
- `src/pages/hooks` for independent shared hooks, queries, and guards
- `src/pages/objects` for pure shared page-layer objects

`src/pages/components` and `src/pages/hooks` are allowlist-style shared areas.
Files there must be flat, without domain subdirectories. Add a new file there
only after it has two or more real page consumers, or when it is route/app shell
infrastructure with no concrete page owner. If a component or hook is used only
by one page and that page's nested components, keep it under that page directory
instead. Tests for shared files may stay next to the file they cover as
`*.test.*` files.

Cross-page collaboration state belongs under `src/pages/stores`. This includes
session state, realtime inbox state, notification counters, and internal browser
storage adapters used by those stores. Keep Zustand stores out of
`src/pages/hooks` and `src/pages/components`; shared hooks in `src/pages/hooks`
may call the stores while owning subscription, query, or refresh orchestration.

Page-layer React hooks belong in the owning page's `hooks` directory or in
`src/pages/hooks` when they meet the shared-code rule above. Page model hooks may
coordinate query and mutation results, but pure parsing, validation, and
business objects should stay in `src/objects`.

## Type Safety

Do not use raw primitives when a domain type exists.

Prefer:

- `Username` over `string`
- `ProblemSlug` over `string`
- `SubmissionId` over `number`
- `ResourceAccessPolicy` over an untyped access object

Rules:

- parse raw JSON, route params, URL search params, and form input at the boundary
- convert into named domain types before business logic
- keep invalid states unrepresentable where practical
- avoid stringly typed status, role, visibility, and lifecycle fields

## Cross-Stack Mirror Rule

Frontend and backend cross-stack object layers must stay aligned for shared
transport and stable business types.

Rules:

- if frontend and backend represent the same stable type, the type name and file basename must match exactly
- request payloads mirror between `frontend/src/objects/<domain>/request` and `backend/src/main/scala/domains/<domain>/objects/request`
- response payloads mirror between `frontend/src/objects/<domain>/response` and `backend/src/main/scala/domains/<domain>/objects/response`
- core domain objects mirror between `frontend/src/objects/<domain>` and `backend/src/main/scala/domains/<domain>/objects`
- shared objects mirror between `frontend/src/objects/shared` and `backend/src/main/scala/shared/objects`
- backend `objects/internal` files are backend-only and skipped by contract alignment
- mirrored object files must not accumulate reducers, HTTP clients, persistence helpers, or workflow orchestration

Prefer:

- `ProblemSummary.scala` and `ProblemSummary.ts`
- `ProblemSetProblemSummary.scala` and `ProblemSetProblemSummary.ts`
- `AccessPolicy.scala` and `AccessPolicy.ts`

Avoid:

- one side using `ProblemListItem` while the other uses `ProblemSummary`
- a mirrored object file quietly becoming a dumping ground for unrelated helpers
- one aggregate file on one side while the same mirrored types are split on the other

Run `node scripts/check-contract-alignment.mjs` when mirrored object files change.

## Functional Core, Imperative Shell

Push side effects to the boundary.

Rules:

- keep parsing, validation, policy, and state transition logic pure where possible
- isolate HTTP, browser storage, time, and other runtime effects in APIs, hooks, page stores, or system helpers
- keep business objects independent of React and UI components
- keep transport conversion in API codecs, not page components

Prefer one more small pure function and one more explicit typed value over a
shared mutable state path or helper that mixes business logic with IO.

## UI Copy and Color

Frontend copy should describe user-facing actions and outcomes, not implementation details.

Rules:

- avoid developer-facing wording such as `domain`, `module`, `route`, or `backend service` in page copy
- prefer direct product language that explains what the user can do
- use `judge system` or direct outcome language instead of internal worker names
- keep one dominant color family per feature area unless there is a clear semantic reason to introduce another accent

## Store Boundaries

State stores are allowed only for real shared state containers.

Rules:

- cross-page page collaboration stores belong in `src/pages/stores`
- browser storage adapters used only by a store belong beside that store, with names that describe the adapter rather than `use-*-store`
- `src/pages/components` and `src/pages/hooks` are for independent shared page code, not store bodies
- stores may hold state, derived flags, and simple state transitions
- stores must not directly own fetch, mutation, permission redirect, or orchestration logic
- query loading belongs in hooks
- mutation side effects belong in hooks or explicit API clients

Prefer a `use...Query` hook that loads data and a `use...Mutation` hook that
performs one request. Add a store only for session state or other cross-page
state that is truly shared.
