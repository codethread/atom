# Wait for User Daemon Home

**Document ID:** `RPS-TASK-001`
**Status:** Blocked
**Plan:** [runtime-plugin-system.plan.md](../runtime-plugin-system.plan.md)

## RPS-TASK-001.P1 Scope

Type: HITL

This is the feature-level blocked marker. Do not start implementation until `devflow/feat/user-daemon-home` has shipped or the human explicitly chooses to proceed against a different runtime connection/loading model.

## RPS-TASK-001.P2 Human unblock condition

- **RPS-TASK-001.H1:** `user-daemon-home` has promoted config-dir daemon worlds, default `init.clj`, `connect!`, and connected `todo daemon repl --stdin`; or
- **RPS-TASK-001.H2:** the human records a new decision in `runtime-plugin-system.plan.md` allowing this feature to proceed earlier.

## RPS-TASK-001.P3 Done when

- **RPS-TASK-001.D1:** The plan's blocked marker has been updated or removed with the reason.
- **RPS-TASK-001.D2:** This task is marked complete in `tasks/index.yml`.
- **RPS-TASK-001.D3:** Downstream task statuses are changed from `blocked` to `pending` only where their `blocked_by` dependencies allow them to run.
