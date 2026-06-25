# Task 5: Wire Go query commands

**Document ID:** `TASK-005`

## TASK-005.P1 Scope

Type: AFK

Wire the Go CLI read/query commands to the daemon JSON socket for `list`, `ready`, and named query execution.

## TASK-005.P2 Must implement exactly

- **TASK-005.MI1:** Implement `list` and `ready` without filters over the JSON socket.
- **TASK-005.MI2:** Implement named query execution with `--query name` and repeated string-valued `--param key=value`, dispatching to allowed daemon operations such as `list-query` and `ready-query`.
- **TASK-005.MI3:** Reject `--where` and `--where EDN` loudly; do not add ad hoc JSON query authoring to the CLI in this task.
- **TASK-005.MI4:** Preserve JSON/human output expectations for empty and non-empty row sets.
- **TASK-005.MI5:** Surface missing named query errors with the missing name and available names when the daemon provides them.
- **TASK-005.MI6:** Add Go tests and at least one integration test proving a query registered through daemon config or REPL-visible daemon API can be consumed by Go CLI `list --query`.

## TASK-005.P3 Done when

- **TASK-005.DW1:** `cd cli; go test ./...` passes.
- **TASK-005.DW2:** Go CLI `list`, `ready`, and `list/ready --query name --param key=value` work against a real daemon.
- **TASK-005.DW3:** No registry mutation/listing command is exposed by the Go CLI or JSON socket.

## TASK-005.P4 Out of scope

- **TASK-005.OS1:** Query registry mutation, listing, or debugging commands.
- **TASK-005.OS2:** Any public CLI EDN or ad hoc query expression syntax.

## TASK-005.P5 References

- **TASK-005.REF1:** `GOCLI-PLAN-001.PH4`, `GOCLI-PLAN-001.TC1`, `GOCLI-PLAN-001.TC3`, `GOCLI-PLAN-001.TC6`.
- **TASK-005.REF2:** `SPEC-002-D003.CC6`, `SPEC-002-D003.CC7`, `SPEC-002-D003.CC13`.
- **TASK-005.REF3:** Query registry contracts `SPEC-002-D002.C1`, `SPEC-002-D002.C3`, `SPEC-002-D002.U2`, and `SPEC-004-D001.C1-C6`.
