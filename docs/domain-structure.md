# Domain-Oriented Structure

The repository is organized around business domains first, with technical layers kept inside each domain.

## Frontend

- `src/features/auth`
  - Authentication, session state, registration, and user settings
- `src/features/site-management`
  - Site-level user management and permission updates
- `src/features/dashboard`
  - Signed-in landing page and top-level entry points
- `src/shared`
  - Cross-feature hooks and routing helpers
- `src/shared/api`
  - Reusable HTTP request helpers and shared client error handling
- `src/shared/domain`
  - Shared frontend domain primitives such as pagination and lifecycle types
- `src/components/ui`
  - Shared presentational UI primitives

## Backend

- `src/main/scala/domains/auth`
  - Auth commands, HTTP routes, models, and persistence for accounts and sessions
- `src/main/scala/domains/system/health`
  - Health endpoint and response model
- `src/main/scala/domains/system/planner`
  - Planner/demo APIs, models, and persistence
- `src/main/scala/domains/shared`
  - Shared models used across domains, including pagination and lifecycle primitives
- `src/main/scala/database`
  - Shared database bootstrap and connection management

## Next Domain Additions

Future OJ modules should follow the same layout:

- `domains/problemset`
- `domains/problem`
- `domains/submission`
- `domains/contest`
- `domains/usergroup`
- `domains/hack`

Each domain should own its:

- `application`
- `http`
- `model`
- `table`
