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
- the same direct, `request`, and `response` pattern for `frontend/src/objects/shared` and `backend/src/main/scala/shared/objects`

The object alignment check first compares object file keys one-to-one. Keys include the domain or `shared` scope, the optional `request` or `response` segment, and the basename without extension, for example:

- `problem/ProblemSlug`
- `problem/request/CreateProblemRequest`
- `shared/PageResponse`
- `shared/response/ErrorResponse`

Scoped files that exist on only one side fail as `frontend-only object file` or `backend-only object file` drift. Frontend object directories are reserved for PascalCase object files, request/response payloads, real object subdomains, and same-object parse/value helpers. Parser helper files, form helpers, display helpers, tests, and barrels belong outside `frontend/src/objects`.

The file-level object check is intentionally not recursive beyond the scoped directories above. Structure boundaries separately reject arbitrary frontend object helper subdirectories and allow only request/response directories plus real object subdomains such as shared access objects.

When a new backend-only object appears in this check, resolve it in this order:

1. If the type is a real HTTP or shared wire payload, add the matching frontend mirror with the same basename and exported type name.
2. If the type is backend-only persistence, state-machine, aggregation, or workflow data, move it under `objects/internal` or into the owning `application` or `table` layer.
3. Add a backend-only exception only when the type cannot reasonably move and is intentionally not serialized to the frontend.

The object alignment check is intentionally structural:

- matched exported type field names and field order must match
- matched exported enum string values must match
- backend `PageResponse[...]` aliases are handled explicitly
- known representation differences are recorded as stable exceptions in the script
- every exception must include a non-empty reason
- unused exceptions fail the check, so stale allowlist entries cannot linger silently

It does not replace runtime tests, endpoint fixtures, or parser tests.

The API alignment check compares `frontend/src/apis/*/*.ts` with `backend/src/main/scala/domains/*/api/*.scala`, with explicit backend-only domains such as judge integration endpoints recorded as script exceptions.
