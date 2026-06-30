# Task 5: Prove linked worktree sharing

**Document ID:** `TASK-MOS-005`

## TASK-MOS-005.P1 Scope

Type: AFK

Add end-to-end coverage proving linked Git worktrees for one repository share the same default weaver/world, while explicit `--config-dir` remains isolated.

## TASK-MOS-005.P2 Must implement exactly

- **TASK-MOS-005.MI1:** Add an integration or smoke scenario that creates a disposable Git repository, initializes Skein once at the repository-canonical default location, creates a linked Git worktree, and runs `strand` from both paths through one running mill.
- **TASK-MOS-005.MI2:** Assert both worktrees report/use the same default `config_dir`, `state_dir`, `data_dir`, and weaver identity when no `--config-dir` is supplied.
- **TASK-MOS-005.MI3:** Assert a command with explicit `--config-dir` in a worktree resolves to a distinct selected world/runtime identity.
- **TASK-MOS-005.MI4:** Include at least one strand mutation/read path across worktrees: create a strand from one worktree and read/list it from the other through the shared weaver.
- **TASK-MOS-005.MI5:** Ensure the scenario uses disposable `XDG_STATE_HOME` and config dirs; do not mutate the user's default state/data worlds.

## TASK-MOS-005.P3 Done when

- **TASK-MOS-005.DW1:** The linked-worktree scenario fails on the old per-worktree default behavior and passes with repository-canonical default selection.
- **TASK-MOS-005.DW2:** Explicit `--config-dir` isolation remains covered.
- **TASK-MOS-005.DW3:** `(cd cli && go test ./...)` passes, and `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke` passes if smoke was changed.

## TASK-MOS-005.P4 Out of scope

- **TASK-MOS-005.OS1:** Testing every unusual Git layout. Unsupported layout fail-loud unit coverage from earlier tasks is enough for MVP.
- **TASK-MOS-005.OS2:** Adding migration support for existing per-worktree `.skein` directories.

## TASK-MOS-005.P5 References

- **TASK-MOS-005.REF1:** [proposal.md](../proposal.md) `MOS-PROP-001.G2`.
- **TASK-MOS-005.REF2:** [mill-owned-source.plan.md](../mill-owned-source.plan.md) `PLAN-MOS-001.PH3` and `PLAN-MOS-001.V2`.
- **TASK-MOS-005.REF3:** `cli/integration_test.go`, `cli/mill_test.go`, `dev/skein/smoke.clj`.
