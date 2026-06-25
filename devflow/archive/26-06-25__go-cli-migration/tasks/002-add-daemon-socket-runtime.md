# Task 2: Add daemon socket runtime

**Document ID:** `TASK-002`

## TASK-002.P1 Scope

Type: AFK

Implement the Clojure daemon side of the JSON Unix socket transport and Go-readable runtime metadata according to the contract from `TASK-001`.

## TASK-002.P2 Must implement exactly

- **TASK-002.MI1:** Extend `src/todo/daemon/runtime.clj` and supporting daemon modules to start a local Unix socket server alongside nREPL and stop/cleanup it with the daemon runtime.
- **TASK-002.MI2:** Extend `src/todo/daemon/metadata.clj` so daemon publication includes Go-readable metadata while preserving existing nREPL metadata compatibility for `todo.client` and REPL workflows during migration.
- **TASK-002.MI3:** Implement JSON request decoding, identity validation, operation allowlist dispatch, success envelopes, domain error envelopes, malformed request errors, and timeout/connection behavior per `TASK-001`.
- **TASK-002.MI4:** Dispatch allowed operations to existing daemon semantic operations in `todo.daemon.api`; do not duplicate SQL/query logic in socket handlers.
- **TASK-002.MI5:** Keep registry mutation/listing/inspection operations unavailable over the JSON socket.
- **TASK-002.MI6:** Add Clojure tests for metadata publication, socket lifecycle, allowlist rejection, success response, domain error response, identity mismatch, and cleanup on stop.

## TASK-002.P3 Done when

- **TASK-002.DW1:** `clojure -M:test` passes with new socket/metadata tests.
- **TASK-002.DW2:** Existing nREPL-backed client/REPL tests still pass.
- **TASK-002.DW3:** Runtime metadata includes socket data and retained nREPL data as specified by `TASK-001`.

## TASK-002.P4 Out of scope

- **TASK-002.OS1:** Go CLI implementation.
- **TASK-002.OS2:** Query registry mutation over JSON.
- **TASK-002.OS3:** Moving Clojure source into an `atom/` folder.

## TASK-002.P5 References

- **TASK-002.REF1:** `GOCLI-PLAN-001.PH2`, `GOCLI-PLAN-001.A1`.
- **TASK-002.REF2:** `SPEC-004-D002.CC1` through `SPEC-004-D002.CC9`.
- **TASK-002.REF3:** `src/todo/daemon/runtime.clj`, `src/todo/daemon/metadata.clj`, `src/todo/daemon/api.clj`.
