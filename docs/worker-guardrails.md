# Worker Guardrails

Back to [Architecture Guardrails](./architecture-guardrails.md).

## Workers

- `backend/src/main/scala/domains/judge`
  - Backend-owned judge orchestration: task claiming, result reporting, task assembly, and HTTP endpoints consumed by workers.
- `backend/src/main/scala/domains/judger`
  - Backend-owned judger registry: worker registration, heartbeat state, and site-management views of registered workers.
- `judger/`
  - Independent worker process for claiming judge tasks, running code, collecting execution results, and reporting results back to the backend.
- `judge-protocol-scala/`
  - Shared Scala protocol module for backend/worker boundary types such as tasks, registration payloads, and reported judge results.

Worker processes are separate applications, not submodules of the backend runtime.

Rules:

- workers must not depend directly on the backend application project
- workers must not import backend `application`, `http`, `table`, or database code
- workers may call backend HTTP endpoints or other stable service boundaries
- if backend and a worker need shared Scala types, extract a small dedicated shared protocol module
- shared protocol modules may contain typed values, parsing, and codecs
- shared protocol modules must not contain SQL, routers, command handlers, or service orchestration
- backend `judge` and `judger` domains may depend on `judge-protocol-scala`, but `judge-protocol-scala` must not depend on backend domains
- judger language runtimes should only prepare executable artifacts and describe the sandbox run command
- judger testcase traversal, output comparison, verdict selection, score or usage aggregation, and result reporting must stay in shared worker logic

Current judger language support:

- `cpp17`
- `python3`, prepared as bytecode and executed from the generated `.pyc` artifact

Prefer:

- `backend -> shared-judge-protocol`
- `judger -> shared-judge-protocol`

Avoid:

- `judger -> backend`
- sharing backend internals just to reuse one model file
- moving worker-specific execution code into backend `shared`

See also:

- [Backend Guardrails](./backend-guardrails.md)
- [Backend Contract Alignment](./backend-contract-alignment.md)
- [Contract Checks](./contract-checks.md)
- [Resource Lifecycle Matrix](./resource-lifecycle-matrix.md)
