# Task 1: Freeze JSON protocol contracts

**Document ID:** `TASK-001`

## TASK-001.P1 Scope

Type: AFK

Define the minimal cross-language contract that lets the Clojure daemon and Go CLI be implemented independently without drifting on metadata, request envelopes, response envelopes, lifecycle semantics, or operation scope.

## TASK-001.P2 Must implement exactly

- **TASK-001.MI1:** Add a feature-local protocol/spec note under `devflow/feat/go-cli-migration/specs/` or extend `specs/daemon-runtime.delta.md` with exact JSON request fields, response fields, error fields, timeout behavior, and connection framing.
- **TASK-001.MI2:** Define Go-readable runtime metadata shape and file strategy, including fields for pid, canonical database path, daemon identity/nonce, Unix socket path, and retained nREPL endpoint data for Clojure clients.
- **TASK-001.MI3:** Define the JSON operation allowlist for this feature: `init`, `add`, `update`, `show`, `list`, `ready`, `list-query`, `ready-query`, `status`, and `stop` only.
- **TASK-001.MI4:** Explicitly exclude `register-query`, `load-queries`, `queries`, and `resolve-query` from the low-privilege JSON socket transport.
- **TASK-001.MI5:** Define `daemon status` as metadata inspection plus socket health/identity check, and `daemon stop` as a socket RPC after identity validation.
- **TASK-001.MI6:** Define the foreground `daemon start` launcher contract for development: Go `todo daemon start` execs the Clojure daemon entrypoint as the long-lived foreground process, forwards `--db` and trusted `--config`, and surfaces startup/config failures loudly.
- **TASK-001.MI7:** Add small JSON golden fixtures for at least one success response and one domain error response that later Clojure and Go tests can consume.

## TASK-001.P3 Done when

- **TASK-001.DW1:** Protocol, metadata, allowlist, status/stop, and foreground start semantics are documented with stable IDs.
- **TASK-001.DW2:** The task leaves no open question about JSON field names, error shape, or whether Go must parse EDN metadata.
- **TASK-001.DW3:** The feature plan references any new protocol note if one is added.

## TASK-001.P4 Out of scope

- **TASK-001.OS1:** Implementing daemon socket code or Go CLI code.
- **TASK-001.OS2:** Defining remote, authenticated, or multi-user daemon protocols.

## TASK-001.P5 References

- **TASK-001.REF1:** `GOCLI-PLAN-001.PH1`, `GOCLI-PLAN-001.TC6`, `GOCLI-PLAN-001.TC7`.
- **TASK-001.REF2:** `SPEC-004-D002.CC5`, `SPEC-004-D002.CC6`, `SPEC-004-D002.Q1`.
- **TASK-001.REF3:** `SPEC-002-D003.CC13`, `TEN-D001.CC1`.
