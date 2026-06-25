# Wire Checks

Use this command to verify that frontend and backend boundary objects have not drifted.

```bash
node scripts/check-object-alignment.mjs
```

Use this command to verify that frontend and backend endpoint API file basenames stay aligned.

```bash
node scripts/check-api-alignment.mjs
```

Current checks cover a narrow object-file surface:

- direct frontend domain `objects/*.ts` files against direct backend domain `objects/*.scala` files
- frontend domain `objects/request/*.ts` files against backend domain `objects/request/*.scala` files
- frontend domain `objects/response/*.ts` files against backend domain `objects/response/*.scala` files
- explicitly mirrored backend `objects/internal/*.scala` plan payloads against direct frontend domain `objects/*.ts` files
- direct shared object files, plus `shared/transport` files for common HTTP success/error bodies

The object alignment check first compares object file keys one-to-one. Keys include the domain or `shared` scope, the optional boundary segment such as `request`, `response`, or `transport`, and the basename without extension, for example:

- `problem/ProblemSlug`
- `problem/request/CreateProblemRequest`
- `shared/PageResponse`
- `shared/transport/ErrorResponse`

Scoped files that exist on only one side fail as `frontend-only object file` or `backend-only object file` drift. Frontend object directories are reserved for PascalCase object files, request/response payloads, shared transport payloads, real object subdomains, and same-object parse/value helpers. Parser helper files, form helpers, display helpers, tests, and barrels belong outside `frontend/src/objects`.

The file-level object check is intentionally not recursive beyond the scoped directories above. Structure boundaries separately reject arbitrary frontend object helper subdirectories and allow only request/response directories plus real object subdomains such as shared access and transport objects.

When a new backend-only object appears in this check, resolve it in this order:

1. If the type is a real HTTP or shared wire payload, add the matching frontend mirror with the same basename and exported type name.
2. If the type is backend-only persistence, state-machine, aggregation, or workflow data, move it under `objects/internal`, the owning `api` support code, or the owning `table` layer.
3. Add a backend-only exception only when the type cannot reasonably move and is intentionally not serialized to the frontend.

The object alignment check is intentionally structural:

- matched exported type field names and field order must match
- matched exported enum string values must match
- backend `PageResponse[...]` aliases are handled explicitly
- known representation differences are recorded as stable exceptions in the script
- every exception must include a non-empty reason
- unused exceptions fail the check, so stale allowlist entries cannot linger silently

It does not replace runtime tests, endpoint fixtures, or parser tests.

The API alignment check compares every direct `frontend/src/apis/*/*.ts` file basename with every direct `backend/src/main/scala/domains/*/api/*.scala` file basename. It intentionally works at the file layer, not only at the `extends *Api` endpoint-object layer, so stray backend API support or input files must be moved out of `api` or recorded as explicit backend-only file exceptions. Explicit backend-only domains such as judge integration endpoints remain script exceptions. Raw binary resource endpoints, such as avatar image reads consumed through an `<img>` URL, may also be explicit backend-only file exceptions.

Realtime is not an API alignment exception because it is not a business domain. The combined `/api/realtime/events` SSE route belongs to the top-level app-shell routing layer and must not be reintroduced under `domains/realtime` or `frontend/src/apis/realtime`.
