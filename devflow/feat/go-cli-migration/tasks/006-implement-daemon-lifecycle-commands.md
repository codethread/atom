# Task 6: Implement daemon lifecycle commands

**Document ID:** `TASK-006`

## TASK-006.P1 Scope

Type: AFK

Implement Go CLI daemon lifecycle commands: foreground `daemon start`, diagnostic `daemon status`, and identity-checked `daemon stop`.

## TASK-006.P2 Must implement exactly

- **TASK-006.MI1:** Implement `todo daemon start [--config <trusted.edn>]` using the foreground launcher contract from `TASK-001`; forward `--db` and trusted config to the Clojure daemon entrypoint.
- **TASK-006.MI2:** Ensure `daemon start` remains the long-lived foreground process and does not detach silently.
- **TASK-006.MI3:** Implement `daemon status` as metadata inspection plus socket health/identity check, returning health, canonical database path, pid, socket endpoint, retained nREPL endpoint data when present, and identity in JSON/human formats.
- **TASK-006.MI4:** Implement `daemon stop` over the JSON socket after identity validation and verify runtime metadata/socket cleanup.
- **TASK-006.MI5:** Fail loudly for missing metadata, stale pid, unreachable socket, daemon identity mismatch, malformed metadata, and trusted config startup errors.
- **TASK-006.MI6:** Add Go/Clojure integration tests or smoke-support tests covering start/status/stop and startup config failure before metadata publication.

## TASK-006.P3 Done when

- **TASK-006.DW1:** Go `todo daemon start` can hold a daemon open in the foreground and load trusted config files.
- **TASK-006.DW2:** Go `todo daemon status --format json` reports socket data and identity.
- **TASK-006.DW3:** Go `todo daemon stop` stops the matched daemon and removes runtime artifacts.

## TASK-006.P4 Out of scope

- **TASK-006.OS1:** Detached/background daemon management.
- **TASK-006.OS2:** Packaging/installers or resolving non-dev Clojure runtime distribution.

## TASK-006.P5 References

- **TASK-006.REF1:** `GOCLI-PLAN-001.PH5`, `GOCLI-PLAN-001.A5`.
- **TASK-006.REF2:** `SPEC-002-D003.CC1`, `SPEC-002-D003.CC9`, `SPEC-002-D003.CC11`.
- **TASK-006.REF3:** `SPEC-004-D002.CC2`, `SPEC-004-D002.CC3`, `SPEC-004-D002.CC6`.
