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
feature-owned `http/api` clients and are expected to reach the backend service.

## Structure

- `src/features/<domain>/model`
  Stable frontend domain/value types.
- `src/features/<domain>/domain`
  Pure parsing, formatting, contract mapping, reducers, and state helpers.
- `src/features/<domain>/http/request`
  Request payload and query types mirrored with backend `application/input`.
- `src/features/<domain>/http/response`
  Response payload types mirrored with backend `application/output` when they are
  transport boundary shapes.
- `src/features/<domain>/http/api`
  One client file per endpoint, with basenames aligned to backend `http/api`.
- `src/features/<domain>/hooks`
  React hooks, query state, mutations, and browser-side orchestration.
- `src/features/<domain>/components`
  Feature-specific UI components.
- `src/features/<domain>/pages`
  Route-level page components.
- `src/shared`
  Cross-feature primitives only after there is a real shared owner.
- `src/components/ui`
  Shared presentational UI primitives.

Current feature domains include `auth`, `blog`, `dashboard`, `judger`, `message`,
`notification`, `problem`, `problemset`, `site-management`, `submission`, `user`,
and `usergroup`.

## Boundary Rules

- `model/` must not import `http/`, `hooks/`, `components/`, or `pages/`.
- `domain/` must not import or re-export `hooks/`, `components/`, or `pages/`.
- React hooks should live in `hooks/`, even when they expose display preferences
  or other feature state.
- Keep transport conversion in domain contract mappers, not in page components.
- Prefer domain-first ownership. Move code to `shared` only after a second real
  consumer appears.

## Validation

From the repository root, run the cheapest relevant checks before committing:

```bash
npm --prefix frontend run typecheck
npm --prefix frontend run lint
node scripts/check-contract-alignment.mjs
node scripts/check-api-alignment.mjs
node scripts/check-structure-boundaries.mjs
```

Run the contract check when mirrored types change. Run the API alignment check when
endpoint files under `http/api` change. Run the structure boundary check after
moving files between `model`, `domain`, `hooks`, `http`, `components`, or `pages`.
