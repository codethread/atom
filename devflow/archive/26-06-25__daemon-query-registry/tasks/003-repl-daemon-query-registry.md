# REPL Daemon Query Registry

**Document ID:** `TASK-003`

## TASK-003.P1 Scope

Type: AFK

Move REPL named-query helpers from process-local registry behavior to daemon-backed registry behavior, while preserving ad hoc query definitions.

## TASK-003.P2 References

- **TASK-003.R1:** [Feature plan](../daemon-query-registry.plan.md)
- **TASK-003.R2:** [REPL API delta](../specs/repl-api.delta.md)
- **TASK-003.R3:** `src/todo/repl.clj`, `test/todo/repl_test.clj`

## TASK-003.P3 Implementation notes

- **TASK-003.I1:** Make `defquery!` register the query in the active daemon registry through `todo.client`.
- **TASK-003.I2:** Make `load-queries!` validate/read one EDN query map and load it into the active daemon registry.
- **TASK-003.I3:** Make `queries` return daemon registry contents.
- **TASK-003.I4:** Make named calls to `query`, `tasks`, and `ready` resolve through daemon memory.
- **TASK-003.I5:** Keep direct vector/map query definitions working without requiring registration.
- **TASK-003.I6:** Ensure symbol and keyword forms of one simple name interoperate with CLI-loaded names.

## TASK-003.P4 Done when

- **TASK-003.D1:** A query registered with REPL `defquery!` is visible through `queries` and usable by name.
- **TASK-003.D2:** REPL named query behavior uses daemon memory, not a local query registry atom.
- **TASK-003.D3:** Missing names throw a clear exception.
- **TASK-003.D4:** Symbol and keyword forms of one simple name resolve to the same daemon entry.
- **TASK-003.D5:** Relevant REPL tests pass.
