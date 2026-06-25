# Init and REPL Examples

**Document ID:** `RPS-TASK-005`
**Status:** Blocked
**Plan:** [runtime-plugin-system.plan.md](../runtime-plugin-system.plan.md)
**Specs:** [daemon-runtime.delta.md](../specs/daemon-runtime.delta.md), [repl-api.delta.md](../specs/repl-api.delta.md), [cli.delta.md](../specs/cli.delta.md)

## RPS-TASK-005.P1 Scope

Type: AFK

Update examples and smoke coverage to show the plugin model through selected config-dir `init.clj` and connected REPL/stdin workflows.

## RPS-TASK-005.P2 Implementation notes

- **RPS-TASK-005.I1:** Add a minimal example `init.clj` using `atom.bootstrap/use-defaults!`.
- **RPS-TASK-005.I2:** Show how users can require a specific blessed library instead of using defaults.
- **RPS-TASK-005.I3:** Show how agents can inspect loaded plugin metadata through `todo daemon repl --stdin`.
- **RPS-TASK-005.I4:** Include a short explanation of blessed vs lower-level vs internal/raw schema use.

## RPS-TASK-005.P3 Done when

- **RPS-TASK-005.D1:** Docs/examples no longer imply core is the only extension path.
- **RPS-TASK-005.D2:** Smoke or integration coverage verifies bootstrap from `init.clj` and plugin introspection through connected REPL/stdin.
