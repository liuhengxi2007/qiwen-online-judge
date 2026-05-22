# Contract Checks

Use this command to verify that backend HTTP-facing Scala models have not drifted from repository-level `contracts/`.

```bash
node scripts/check-contract-alignment.mjs
```

Use this command to verify that frontend and backend endpoint API file basenames stay aligned.

```bash
node scripts/check-api-alignment.mjs
```

Current checks cover:

- shared transport models
- shared lifecycle enum values
- auth transport models
- blog transport models
- judger transport models
- problem transport models
- problemset transport models
- submission transport models
- usergroup transport models
- notification transport models

The check is intentionally structural:

- field names and field order must match
- enum string values must match
- backend alias differences are handled explicitly where needed

It does not replace runtime tests or schema generation.

It is a low-cost drift detector for normal repository changes.

The API alignment check compares `frontend/src/features/*/http/api/*.ts` with `backend/src/main/scala/domains/*/http/api/*.scala`, ignoring frontend `*-client.ts` barrels and explicit backend-only domains such as judge integration endpoints.
