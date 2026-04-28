# Architecture Guardrails

The repository is organized around business domains first, with technical layers kept inside each domain.

Use this file as the entry point, then continue through the linked topic documents instead of treating one large file as the only source of truth.

## Core Guides

- [Frontend Guardrails](./frontend-guardrails.md)
  Frontend structure, shared-code rules, type-safety rules, hook discipline, store boundaries, and feature file templates.
- [Backend Guardrails](./backend-guardrails.md)
  Backend domain layout, file-role templates, functional-core boundaries, shared-layer limits, and HTTP planner rules.
- [Worker Guardrails](./worker-guardrails.md)
  Boundaries for the `judger` worker and shared protocol extraction rules.

## Related Docs

- [Backend Contract Alignment](./backend-contract-alignment.md)
- [Contract Checks](./contract-checks.md)
- [File Upload Capability](./file-upload-capability.md)
- [HTTP Planner Protocol](./http-planner-protocol.md)
- [Problem Data Storage Migration](./problem-data-storage-migration.md)
- [Resource Lifecycle Matrix](./resource-lifecycle-matrix.md)

## Rule

When updating architecture rules:

- prefer editing the narrowest topical document above
- keep explicit links between related documents
- avoid reintroducing one monolithic guardrail file
