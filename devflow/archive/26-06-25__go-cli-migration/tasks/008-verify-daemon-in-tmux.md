# Task 8: Verify daemon in tmux

**Document ID:** `TASK-008`

## TASK-008.P1 Scope

Type: HITL

Manually verify the migrated Go CLI against a long-lived foreground daemon held open in tmux, then record evidence before docs/spec promotion.

## TASK-008.P2 Must implement exactly

- **TASK-008.MI1:** Start a named tmux session such as `agent-go-cli-migration` and run `./cli/bin/todo --db /tmp/todo-go-cli-tmux.sqlite daemon start` in the foreground, including `--config <trusted.edn>` if needed to register a named query.
- **TASK-008.MI2:** From a separate shell, use the Go `todo` binary to run `daemon status`, `init`, `add`, `update`, `ready`, `list --query ...`, `show`, and `daemon stop` against the same database.
- **TASK-008.MI3:** Confirm the daemon remains live during separate CLI invocations and exits/cleans runtime artifacts only after `daemon stop`.
- **TASK-008.MI4:** Capture the salient tmux output/status and append a Developer Note to `go-cli-migration.plan.md` with the database path, session name, commands exercised, result summary, and any cleanup performed.
- **TASK-008.MI5:** Clean up tmux session, temporary SQLite files, runtime metadata, and socket artifacts.

## TASK-008.P3 Done when

- **TASK-008.DW1:** The plan contains a Developer Note recording successful manual tmux verification evidence.
- **TASK-008.DW2:** The daemon foreground behavior, cross-process Go CLI behavior, status/stop behavior, and cleanup behavior have been verified manually.
- **TASK-008.DW3:** No tmux session or generated runtime/database artifacts remain after cleanup.

## TASK-008.P4 Out of scope

- **TASK-008.OS1:** Implementing fixes discovered during verification; create or run a repair task if validation fails.
- **TASK-008.OS2:** Promoting docs/specs before verification evidence is recorded.

## TASK-008.P5 References

- **TASK-008.REF1:** `GOCLI-PLAN-001.V4`.
- **TASK-008.REF2:** `AGENTS.md` manual tmux verification guidance.
- **TASK-008.REF3:** tmux skill for durable terminal session operation.
