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
- frontend feature `http/request` files against backend `application/input`
- frontend feature `http/response` files against backend `application/output`
- mirrored feature `model` files when both sides expose the same key

The contract alignment check is intentionally structural:

- field names and field order must match
- enum string values must match
- backend `PageResponse[...]` aliases are handled explicitly
- known representation differences are recorded as stable exceptions in the script

It does not replace runtime tests, endpoint fixtures, or parser tests.

The API alignment check compares `frontend/src/features/*/http/api/*.ts` with `backend/src/main/scala/domains/*/http/api/*.scala`, ignoring frontend `*-client.ts` barrels and explicit backend-only domains such as judge integration endpoints.
