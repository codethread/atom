# Task 7: Update smoke and validation

**Document ID:** `TASK-007`

## TASK-007.P1 Scope

Type: AFK

Update project smoke/validation flows so they exercise the Go `todo` CLI over the daemon JSON socket while retaining REPL validation for rich Clojure workflows.

## TASK-007.P2 Must implement exactly

- **TASK-007.MI1:** Update `dev/todo/smoke.clj` or add an equivalent smoke path that builds/uses `./cli/bin/todo` and invokes it from separate processes.
- **TASK-007.MI2:** Smoke must start a daemon, initialize storage, create tasks, add dependencies, update status, query ready/list data, consume a named query registered through trusted daemon config or REPL-visible daemon API, inspect status, and stop the daemon.
- **TASK-007.MI3:** Remove public CLI EDN usage from smoke; use JSON for machine assertions and human output only where explicitly tested.
- **TASK-007.MI4:** Retain or update REPL smoke coverage so EDN-rich query authoring/debugging remains validated through Clojure REPL/config workflows.
- **TASK-007.MI5:** Update existing Clojure CLI tests such as `test/todo/cli_test.clj` so they clearly reflect retained/internal behavior versus behavior replaced by the Go CLI.
- **TASK-007.MI6:** Add or update validation instructions so agents can run Clojure tests, Go tests, smoke, and the separate manual tmux daemon verification in `TASK-008`.
- **TASK-007.MI7:** Ensure smoke cleanup removes generated SQLite, runtime metadata, socket files, and built CLI artifacts if they are considered generated.

## TASK-007.P3 Done when

- **TASK-007.DW1:** Clojure tests pass with `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test`.
- **TASK-007.DW2:** Go tests pass with `cd cli; go test ./...`.
- **TASK-007.DW3:** Smoke passes with the Go CLI path and does not assert on EDN CLI output.
- **TASK-007.DW4:** `git status --short` does not show generated SQLite/runtime/socket artifacts after successful validation.

## TASK-007.P4 Out of scope

- **TASK-007.OS1:** Durable documentation/spec promotion; this task updates runnable validation paths only.
- **TASK-007.OS2:** Full packaging or release automation.

## TASK-007.P5 References

- **TASK-007.REF1:** `GOCLI-PLAN-001.P6`, `GOCLI-PLAN-001.PH5`.
- **TASK-007.REF2:** Existing smoke file `dev/todo/smoke.clj`.
- **TASK-007.REF3:** `AGENTS.md` validation and tmux verification guidance.
