# Bootstrap Prelude Namespaces

**Document ID:** `RPS-TASK-004`
**Status:** Blocked
**Plan:** [runtime-plugin-system.plan.md](../runtime-plugin-system.plan.md)
**Specs:** [repl-api.delta.md](../specs/repl-api.delta.md)

## RPS-TASK-004.P1 Scope

Type: AFK

Add the initial blessed user-facing namespaces for plugin ergonomics: `atom.bootstrap` and `atom.prelude` or the final names chosen during spec review.

## RPS-TASK-004.P2 Implementation notes

- **RPS-TASK-004.I1:** Add `atom.bootstrap/use-defaults!` for side-effectful recommended setup.
- **RPS-TASK-004.I2:** Add `atom.prelude` for optional interactive convenience imports.
- **RPS-TASK-004.I3:** Keep first defaults intentionally small: load/register base Atom library metadata and prepare the convention for future blessed query/graph/view libraries.
- **RPS-TASK-004.I4:** Do not implement query/graph/view libraries in this task.

## RPS-TASK-004.P3 Done when

- **RPS-TASK-004.D1:** Namespaces load from the configured source checkout.
- **RPS-TASK-004.D2:** `(atom.bootstrap/use-defaults!)` is idempotent enough for daemon startup/reload workflows.
- **RPS-TASK-004.D3:** Tests cover namespace loadability and metadata effects.
