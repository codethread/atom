# Task 9: Promote docs and specs

**Document ID:** `TASK-009`

## TASK-009.P1 Scope

Type: AFK

Update durable docs/specs and feature status after the Go CLI migration is implemented and validated.

## TASK-009.P2 Must implement exactly

- **TASK-009.MI1:** Merge `SPEC-002-D003` into `devflow/specs/cli.md`, updating the public entrypoint to `todo`, machine format to JSON-only, removal of `--where EDN`, `--client-config`, named query usage, and daemon lifecycle/status behavior.
- **TASK-009.MI2:** Merge `SPEC-004-D002` into `devflow/specs/daemon-runtime.md`, documenting JSON Unix socket transport, Go-readable metadata, nREPL retention, operation allowlist, and error/identity behavior.
- **TASK-009.MI3:** Merge `TEN-D001` into `devflow/TENETS.md` without renumbering existing tenets.
- **TASK-009.MI4:** Update `README.md`, `AGENTS.md`, and `devflow/README.md` command examples from `clojure -M:todo` public CLI usage to the Go `todo` CLI where appropriate; keep Clojure/EDN examples only for daemon config, REPL, or development workflows.
- **TASK-009.MI5:** Mark feature-local deltas as `Merged` after promotion and update the plan Developer Notes with shipped scope and any cut/deferred scope.
- **TASK-009.MI6:** If validation is complete and the user requests finish/archive, follow the devflow finish/archive procedure for moving implemented RFCs and the feature folder.

## TASK-009.P3 Done when

- **TASK-009.DW1:** Root specs and tenets reflect shipped behavior and no longer rely on feature-local deltas as the only source of truth.
- **TASK-009.DW2:** User/agent docs no longer recommend EDN for public CLI machine output.
- **TASK-009.DW3:** Feature-local deltas are marked `Merged`, or any unmerged delta has an explicit Developer Note explaining why.

## TASK-009.P4 Out of scope

- **TASK-009.OS1:** Implementing missing Go/daemon behavior; this task assumes Tasks 1-8 are complete.
- **TASK-009.OS2:** Archiving without user confirmation if project workflow requires an explicit finish/archive request.

## TASK-009.P5 References

- **TASK-009.REF1:** `GOCLI-PLAN-001.PH6`.
- **TASK-009.REF2:** `SPEC-002-D003`, `SPEC-004-D002`, `TEN-D001`.
- **TASK-009.REF3:** Devflow finish/archive procedure in the devflow skill.
