# Contract Checks

Use this command to verify that backend HTTP-facing Scala models have not drifted from repository-level `contracts/`.

```bash
node scripts/check-contract-alignment.mjs
```

Current checks cover:

- shared transport models
- shared lifecycle enum values
- auth transport models
- problem transport models
- problemset transport models

The check is intentionally structural:

- field names and field order must match
- enum string values must match
- backend alias differences are handled explicitly where needed

It does not replace runtime tests or schema generation.

It is a low-cost drift detector for normal repository changes.
