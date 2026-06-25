# Task 4: Wire Go task commands

**Document ID:** `TASK-004`

## TASK-004.P1 Scope

Type: AFK

Wire the Go CLI task commands to the daemon JSON socket for the core task lifecycle: `init`, `add`, `update`, and `show`.

## TASK-004.P2 Must implement exactly

- **TASK-004.MI1:** Implement Go runtime metadata discovery and socket client calls for allowed task operations, using the JSON contract from `TASK-001`.
- **TASK-004.MI2:** Implement `init`, `add`, `update`, and `show` command execution over the socket; these commands must not shell out to the Clojure CLI and must not open SQLite.
- **TASK-004.MI3:** Preserve current CLI parsing behavior for task titles, generated id output in human mode for `add`, first-class status values, repeated `--attr key=value`, and repeated `--edge edge-type:to-id` on `update`.
- **TASK-004.MI4:** Implement JSON and human output for task command results using normalized JSON-friendly row shapes from the daemon.
- **TASK-004.MI5:** Map daemon/domain errors and transport errors to non-zero exits with useful messages.
- **TASK-004.MI6:** Add Go tests using fake socket/server fixtures for success, daemon error, malformed response, stale/missing metadata, and socket unreachable cases.

## TASK-004.P3 Done when

- **TASK-004.DW1:** `cd cli; go test ./...` passes.
- **TASK-004.DW2:** Integration coverage proves Go `todo init`, `add`, `update`, and `show` work against a real daemon socket.
- **TASK-004.DW3:** The task command path has no dependency on `clojure -M:todo` except where test setup starts the daemon.

## TASK-004.P4 Out of scope

- **TASK-004.OS1:** `list`, `ready`, named query behavior, and daemon lifecycle commands.
- **TASK-004.OS2:** Query registry mutation or inspection.

## TASK-004.P5 References

- **TASK-004.REF1:** `GOCLI-PLAN-001.PH4`.
- **TASK-004.REF2:** `SPEC-002-D003.CC3`, `SPEC-002-D003.CC4`, `SPEC-002-D003.CC5`, `SPEC-002-D003.CC10`.
- **TASK-004.REF3:** Current behavior in `src/todo/cli.clj` and `test/todo/cli_test.clj`.
