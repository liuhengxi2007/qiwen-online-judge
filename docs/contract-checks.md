# Contract Checks

Use this command to verify that frontend and backend boundary objects have not drifted.

```bash
node scripts/check-contract-alignment.mjs
```

Use this command to verify that frontend and backend endpoint API file basenames stay aligned.

```bash
node scripts/check-api-alignment.mjs
```

Current checks cover:

- shared transport objects
- shared access and lifecycle enum values
- frontend domain `objects/request` files against backend `objects/request`
- frontend domain `objects/response` files against backend `objects/response`
- mirrored domain `objects` files when both sides expose the same key
- backend `objects/internal` is skipped because it is backend-only collaboration state, not an HTTP contract

The contract alignment check is intentionally structural:

- field names and field order must match
- enum string values must match
- backend `PageResponse[...]` aliases are handled explicitly
- known representation differences are recorded as stable exceptions in the script
- every exception must include a non-empty reason
- unused exceptions fail the check, so stale allowlist entries cannot linger silently

It does not replace runtime tests, endpoint fixtures, or parser tests.

The API alignment check compares `frontend/src/apis/*/*.ts` with `backend/src/main/scala/domains/*/http/api/*.scala`, with explicit backend-only domains such as judge integration endpoints recorded as script exceptions.
