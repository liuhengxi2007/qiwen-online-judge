# HTTP API Object Protocol

This repository uses normal REST routes with per-endpoint API objects in each backend domain.

It is not a global planner router and not a standalone `planner` business domain.

## Rule

Keep normal REST URLs, then model each HTTP use case as one typed API object.

Preferred split:

- `*Router.scala`
  thin domain route aggregator that registers endpoint API objects in a list
- `api/<Name>.scala`
  one REST endpoint API object or dependency-carrying case class, including method, path, input decode, and the complete `plan(...)` orchestration body
- object/request/response companion objects
  Circe encoders/decoders for backend HTTP request and response wire formats
- auth-owned API object router and session resolver in `domains/auth/api`
  match method/path, decode input, resolve session cookies to actors at the HTTP boundary, open the transaction, call `plan(...)`, and encode success/error responses
- small HTTP helpers such as `HttpApiError`
  convert known failures to code-only API responses

## Execution Model

API objects should return typed outputs unless the endpoint must customize the raw HTTP response, such as file downloads or cookie-setting auth responses.

For authenticated APIs:

- session cookie resolution happens in `domains/auth/api/SessionResolver`
- `plan(...)` receives `AuthUser` or `SiteManagerUser`, never raw cookies or session tokens
- transaction-backed APIs receive a `Connection`
- the visible business workflow belongs directly in the API object's `plan(...)`

Allowed helper calls from `plan(...)`:

- table operations
- value constructors/parsers
- pure rule/decision helpers
- storage or event adapters
- small HTTP response/error helpers

Do not hide endpoint orchestration behind:

- `*HttpPlans.scala`
- `*HttpPlanDefinitions.scala`
- `*Commands.scala`
- `*QueryCommands.scala`
- `*MutationCommands.scala`
- service-style wrappers that only exist to split one endpoint's business flow away from its API object

## Errors

Known failures should raise `HttpApiError` with an `ApiMessage` code. The HTTP response body should use:

- `code = Some("api.error...")`
- `message = None`
- `params = ...`

Raw `message` fallback is only for decode failures, validation strings that have not yet been promoted to i18n codes, and unknown failures.

## Non-Goals

Do not:

- add a generic `planner` domain
- replace REST routes with a single planner endpoint
- move SQL into API objects
- move HTTP response/cookie concerns into table or object files
