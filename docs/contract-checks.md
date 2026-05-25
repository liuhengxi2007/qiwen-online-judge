# Contract Checks

Use this command to verify that frontend and backend boundary models have not drifted.

```bash
node scripts/check-contract-alignment.mjs
```

Use this command to verify that frontend and backend endpoint API file basenames stay aligned.

```bash
node scripts/check-api-alignment.mjs
```

Current checks cover:

- shared transport models
- shared access and lifecycle enum values
- frontend feature `model/request` files against backend `model/request`
- frontend feature `model/response` files against backend `model/response`
- mirrored feature `model` files when both sides expose the same key
- backend `model/internal` is skipped because it is backend-only collaboration state, not an HTTP contract

The contract alignment check is intentionally structural:

- field names and field order must match
- enum string values must match
- backend `PageResponse[...]` aliases are handled explicitly
- known representation differences are recorded as stable exceptions in the script
- every exception must include a non-empty reason
- unused exceptions fail the check, so stale allowlist entries cannot linger silently

It does not replace runtime tests, endpoint fixtures, or parser tests.

The API alignment check compares `frontend/src/features/*/http/api/*.ts` with `backend/src/main/scala/domains/*/http/api/*.scala`, with explicit backend-only domains such as judge integration endpoints recorded as script exceptions.
