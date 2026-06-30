# Task 1: Canonicalize default repo world

**Document ID:** `TASK-MOS-001`

## TASK-MOS-001.P1 Scope

Type: AFK

Implement repository-canonical default world discovery so linked Git worktrees for the same repository resolve to the same default selected config-dir.

## TASK-MOS-001.P2 Must implement exactly

- **TASK-MOS-001.MI1:** Update `cli/internal/config` so default world/bootstrap discovery uses the repository canonical route from `git rev-parse --path-format=absolute --git-common-dir`, not the current worktree root from `--show-toplevel`.
- **TASK-MOS-001.MI2:** For supported normal and linked-worktree layouts, require the absolute common Git dir basename to be `.git`, derive the canonical repository root as that `.git` directory's parent, and select `<canonical-root>/.skein`.
- **TASK-MOS-001.MI3:** Fail loudly for no-Git, bare repositories, submodules, or any non-standard layout where the absolute common Git dir is not a canonical repository `.git` directory. Preserve the existing behavior that explicit `--config-dir` bypasses default repo discovery.
- **TASK-MOS-001.MI4:** Keep `RuntimeWorld(configDir)` as the selected-world-to-XDG runtime/data hash boundary; do not move runtime state into `.skein`.
- **TASK-MOS-001.MI5:** Add Go tests in `cli/internal/config` that create a real Git repository plus linked worktree and assert both cwd values resolve to the same default config-dir/runtime identity.

## TASK-MOS-001.P3 Done when

- **TASK-MOS-001.DW1:** A cwd in the main worktree and a cwd in a linked worktree resolve to the same default selected config-dir.
- **TASK-MOS-001.DW2:** Explicit `--config-dir` still resolves relative to the caller cwd when relative and is not canonicalized through Git repository discovery.
- **TASK-MOS-001.DW3:** Relevant Go config tests pass with `(cd cli && go test ./internal/config)`.

## TASK-MOS-001.P4 Out of scope

- **TASK-MOS-001.OS1:** Removing `source` from `config.json`; task 2 owns that.
- **TASK-MOS-001.OS2:** Mill source resolution or process launch changes; task 3 owns those.

## TASK-MOS-001.P5 References

- **TASK-MOS-001.REF1:** [proposal.md](../proposal.md) `MOS-PROP-001.S3`.
- **TASK-MOS-001.REF2:** [mill-owned-source.plan.md](../mill-owned-source.plan.md) `PLAN-MOS-001.A1` and `PLAN-MOS-001.PH1`.
- **TASK-MOS-001.REF3:** `cli/internal/config/config.go`, `cli/internal/config/bootstrap.go`, `cli/internal/config/runtime.go`, `cli/internal/config/config_test.go`.
