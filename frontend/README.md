# Qiwen Online Judge Frontend

This is the Vite + React 19 frontend for Qiwen Online Judge.

## Commands

```bash
npm install
npm run dev
npm run typecheck
npm run lint
npm run build
```

`npm run dev` starts the Vite development server. API calls are implemented under
domain-owned `src/apis/<domain>` clients and are expected to reach the backend service.

## Structure

- `src/apis/<domain>`
  One endpoint client per file, with basenames aligned to backend domain `api`.
- `src/apis/<domain>/codecs`
  HTTP boundary codecs for endpoint payloads and domain object wire values.
- `src/objects/<domain>`
  Stable frontend business objects, parsers, pure display helpers, pure form helpers, and mirrored object types.
- `src/objects/<domain>/request`
  Request payload types mirrored with backend `objects/request`.
- `src/objects/<domain>/response`
  Response payload types mirrored with backend `objects/response`.
- `src/objects/shared`
  Cross-domain business objects and pure helpers after there is a real shared owner.
- `src/pages/<PageName>/index.tsx`
  Route-level page components.
- `src/pages/components`, `src/pages/hooks`, `src/pages/objects`
  Reusable page-layer UI, hooks, stores, route policies, URL/search-param state, and display-only state.
  See `page-shared-consumers.md` for the current page consumer inventory.
- `src/system`
  Runtime helpers such as HTTP client support and i18n.
- `src/components/ui`
  Shared presentational UI primitives.

Current domains include `auth`, `blog`, `dashboard`, `judger`, `message`,
`notification`, `problem`, `problemset`, `site-management`, `submission`, `user`,
and `usergroup`.

## Boundary Rules

- `objects` must not import `apis`, `system`, `pages`, or `components/ui`.
- `objects/shared` must not import domain objects.
- `system` may import `objects/shared` only.
- `apis` may import objects, peer API codecs, and `system/api`; never pages.
- `components/ui` may import `system/i18n` for generic labels; never pages or domain objects.
- `pages/objects` is page-layer only and may only be imported by `pages`.
- Route pages compose APIs, objects, system helpers, UI components, page components, page hooks, and page objects.

## Validation

From the repository root, run the cheapest relevant checks before committing:

```bash
npm --prefix frontend run typecheck
npm --prefix frontend run lint
node scripts/check-object-alignment.mjs
node scripts/check-api-alignment.mjs
node scripts/check-structure-boundaries.mjs
```

Run the object alignment check when mirrored object types change. Run the API alignment
check when endpoint files under `src/apis` change. Run the structure boundary
check after moving files across frontend layers.
