# Planner Patterns

The former `domains/system/planner` module was removed because it was demo code, not part of the Online Judge business domains.

Two structural ideas are still worth reusing in future domains.

## Capability Registry

Use a registry-style structure only when the system truly has a closed set of named capabilities.

Examples:

- judge task types
- scoring strategies
- hack strategies
- statement renderers

Use this pattern when:

- capabilities are selected by name
- the set is enumerable
- registration is clearer than large route-level branching

Do not use it for ordinary resource CRUD domains such as:

- problem
- problemset
- contest
- submission

## Thin Boundary Execution

The useful part of the old planner router was not the planner domain itself.

The useful part was:

1. decode transport input at the boundary
2. dispatch to a selected capability
3. keep storage access explicit when required
4. map errors back to transport responses

This execution style is reusable for future command-style modules, but it should live inside the owning business domain.

## Non-Pattern

Do not reintroduce a generic `planner` domain as a dumping ground for miscellaneous APIs.

If a feature belongs to:

- auth
- problem
- problemset
- contest
- submission
- hack

then it should be implemented inside that domain directly.
