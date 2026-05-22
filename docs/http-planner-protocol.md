# HTTP Planner Protocol

This repository uses a local HTTP planner protocol inside each backend domain.

It is not a global planner router and not a standalone `planner` business domain.

## Rule

Keep normal REST routers, then model each HTTP use case as a typed plan.

Preferred split:

- `*Router.scala`
  thin domain route aggregator only
- `api/<Name>.scala`
  one REST endpoint route fragment, including path matching, request parsing/decoding, executor call, and response mapping for that endpoint
- `*HttpPlans.scala`
  typed endpoint plans
- `*HttpPlanDefinitions.scala`
  registers plans and binds `Output => Response[IO]`
- shared executor in `domains/shared/http`
  runs auth checks, request decoding, and transaction boundaries

## Execution Model

For the main business domains (`problem`, `problemset`, `submission`, `usergroup`):

- `PlainAuthenticatedHttpPlan`
  no transaction connection
- `TransactionAuthenticatedHttpPlan`
  executes inside `withTransactionConnection`

Plans should return typed outputs, not raw `Response[IO]`.
Response mapping belongs in `*HttpPlanDefinitions.scala` and `*HttpResponses.scala`.
Shared HTTP-only helpers belong in `http/utils`, not beside endpoint API files.

## Auth Exception

`auth/http` uses a richer local protocol because it needs:

- public plans
- authenticated plans
- site-manager plans
- plain and transaction variants for each

That complexity should stay inside `domains/auth/http`, not leak into unrelated domains.

## Non-Goals

Do not:

- add a generic `planner` domain
- replace REST routes with a single planner endpoint
- move business orchestration, policy logic, or SQL into plans

The planner layer is an HTTP execution protocol, not a replacement for `application`.
