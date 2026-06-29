# Repository Guidelines

## Project Structure & Module Organization

- `frontend/`: Vite + React 19 app. Business objects live in `src/objects/<domain>`, endpoint clients in `src/apis/<domain>`, route/page code in `src/pages`, runtime helpers in `src/system`, and generic UI in `src/components/ui`.
- `backend/`: Scala 3 + Cats Effect + http4s service. Most domains are split into `api`, `routes`, `objects`, and `table` under `src/main/scala/domains/<domain>`, with optional allowlisted `utils`.
- `judger/` and `judge-protocol/`: judge worker and shared protocol code.
- `docs/architecture-guardrails.md`: source of truth for layering, type mirroring, and shared-code rules.
- `scripts/`: maintenance scripts such as `check-object-alignment.mjs` and `calc-loc.mjs`.

## Build, Test, and Development Commands

- Frontend dev server: `cd frontend && npm run dev`
- Frontend production build: `cd frontend && npm run build`
- Frontend type check: `cd frontend && npm run typecheck`
- Frontend lint: `cd frontend && npm run lint`
- Backend run: `cd backend && sbt run`
- Backend compile check: `cd backend && sbt compile`
- Object alignment check: `node scripts/check-object-alignment.mjs`

## Coding Style & Naming Conventions

- Prefer domain-first ownership. Put code in `objects/shared` only after a second real consumer appears.
- Frontend uses TypeScript, React function components, hooks, and reducer-style state files such as `*-state.ts`.
- Backend uses Scala 3 objects/case classes, explicit `IO[...]` at effect boundaries, and domain-specific ADTs for results.
- Mirrored cross-stack object types must match exactly in basename and type name, for example `ProblemSummary.scala` and `ProblemSummary.ts`.
- Avoid raw primitives when a domain type exists (`Username`, `ProblemSlug`, `SubmissionId`).
- Code comments should be written in Chinese. Keep comments concise and explain intent or non-obvious behavior, not line-by-line mechanics.

## Testing Guidelines

- Run `npm run typecheck` before frontend commits and `sbt compile` before backend commits.
- If you touch mirrored frontend/backend types, also run `node scripts/check-object-alignment.mjs`.
- Keep tests or checks close to the changed layer; do not skip validation for “small” refactors.

## Commit & Pull Request Guidelines

- Follow the existing history: short imperative subjects such as `Unify displayed users as user identities`.
- Keep each commit scoped to one concern. Separate refactors, i18n copy changes, and behavior changes when practical.
- PRs should describe user-visible impact, validation performed, and any schema/API consequences. Include screenshots for UI changes.

## Security & Configuration Tips

- Do not leak existence of protected resources unless the feature explicitly requires it.
- Prefer `403` for public user-profile access violations and `404` for hidden resource-instance access.
- Use environment variables for database configuration; do not commit local secrets.

## Reducing Unnecessary Token Usage

- Read narrowly. Prefer `rg` and targeted `sed -n` over opening entire large files.
- When working in `docs/`, follow explicit links such as `See also` and open only the documents needed for the current task.
- Do not restate file contents in chat when a short summary is enough.
- Reuse existing domain patterns before exploring alternatives. Check the owning domain first, then `shared`.
- Avoid duplicate exploration. If a rule already exists in `docs/architecture-guardrails.md`, follow it instead of re-deriving it.
- Keep changes local. Edit the smallest set of files that can express the behavior clearly.
- Validate with the cheapest effective command first:
  - frontend: `npm run typecheck`
  - backend: `sbt compile`
  - mirrored types: `node scripts/check-object-alignment.mjs`
- When investigating UI copy or routing, search exact keys or route fragments first, for example:
  - `rg -n "site-manage-denied|toForbiddenRedirect" frontend/src`
  - `rg -n "common.cancel" frontend/src`

## Collaboration Expectations

- If a requested design or implementation looks technically unsound, say so clearly and explain why instead of forcing it through unchanged.
