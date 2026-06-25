# Plugin Contract Specs

**Document ID:** `RPS-TASK-002`
**Status:** Blocked
**Plan:** [runtime-plugin-system.plan.md](../runtime-plugin-system.plan.md)
**Specs:** [daemon-runtime.delta.md](../specs/daemon-runtime.delta.md), [repl-api.delta.md](../specs/repl-api.delta.md), [cli.delta.md](../specs/cli.delta.md)

## RPS-TASK-002.P1 Scope

Type: AFK

Finalize the feature-local specs for trusted plugins, blessed libraries, stability/coupling tiers, and the deferred `straight.el`-inspired package direction.

## RPS-TASK-002.P2 Implementation notes

- **RPS-TASK-002.I1:** Ensure specs make clear that blessed libraries are recommended/maintained paths, not restrictions.
- **RPS-TASK-002.I2:** Preserve user autonomy to use lower-level namespaces or raw SQLite schema with explicit compatibility cost.
- **RPS-TASK-002.I3:** Keep package recipes/lockfiles as future direction only.
- **RPS-TASK-002.I4:** Do not add implementation beyond spec/doc edits in this task.

## RPS-TASK-002.P3 Done when

- **RPS-TASK-002.D1:** Feature deltas are internally consistent and no longer leave core plugin model wording ambiguous.
- **RPS-TASK-002.D2:** Plan Developer Notes record any resolved naming or duplicate-registration decisions.
