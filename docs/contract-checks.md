# Contract Checks

Use this command to verify that frontend and backend boundary objects have not drifted.

```bash
node scripts/check-contract-alignment.mjs
```

Use this command to verify that frontend and backend endpoint API file basenames stay aligned.

```bash
node scripts/check-api-alignment.mjs
```

Current checks cover a narrow object-file surface:

- direct frontend domain `objects/*.ts` files against direct backend domain `objects/*.scala` files
- frontend domain `objects/request/*.ts` files against backend domain `objects/request/*.scala` files
- frontend domain `objects/response/*.ts` files against backend domain `objects/response/*.scala` files
- the same direct, `request`, and `response` pattern for `frontend/src/objects/shared` and `backend/src/main/scala/shared/objects`

The contract alignment check first compares object file keys one-to-one. Keys include the domain or `shared` scope, the optional `request` or `response` segment, and the basename without extension, for example:

- `problem/ProblemSlug`
- `problem/request/CreateProblemRequest`
- `shared/PageResponse`
- `shared/response/ErrorResponse`

Scoped files that exist on only one side fail as `frontend-only object file` or `backend-only object file` drift. This includes frontend helper, parser, form, and test files placed directly under `frontend/src/objects/<domain>` or `frontend/src/objects/shared`, because those directories are reserved for mirrored object files.

The file-level check is intentionally not recursive beyond the scoped directories above. Nested helper or implementation folders such as `objects/internal`, `objects/access`, or other arbitrary subdirectories are outside this check.

The contract alignment check is intentionally structural:

- matched exported type field names and field order must match
- matched exported enum string values must match
- backend `PageResponse[...]` aliases are handled explicitly
- known representation differences are recorded as stable exceptions in the script
- every exception must include a non-empty reason
- unused exceptions fail the check, so stale allowlist entries cannot linger silently

It does not replace runtime tests, endpoint fixtures, or parser tests.

The API alignment check compares `frontend/src/apis/*/*.ts` with `backend/src/main/scala/domains/*/http/api/*.scala`, with explicit backend-only domains such as judge integration endpoints recorded as script exceptions.
