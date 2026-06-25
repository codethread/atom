# Task 3: Add Go CLI skeleton

**Document ID:** `TASK-003`

## TASK-003.P1 Scope

Type: AFK

Create the `cli/` Go module and command skeleton for the native `todo` CLI, including config/default handling and validation before operation wiring.

## TASK-003.P2 Must implement exactly

- **TASK-003.MI1:** Create `cli/go.mod` and a buildable entrypoint at `cli/cmd/todo/main.go`.
- **TASK-003.MI2:** Add an internal package structure for command parsing, client config, daemon socket client stubs, output formatting, and version/build metadata.
- **TASK-003.MI3:** Implement global `--db`, `--format human|json`, and `--client-config` handling; keep `daemon start --config` separate for trusted daemon startup config.
- **TASK-003.MI4:** Implement XDG JSON config loading for low-privilege defaults such as database path and default output format, with explicit flags taking precedence.
- **TASK-003.MI5:** Implement command tree/help for `init`, `add`, `update`, `show`, `list`, `ready`, and `daemon start|status|stop`.
- **TASK-003.MI6:** Reject `--format edn`, `--where`, malformed config, unsupported config keys, invalid statuses, malformed `--attr`, malformed `--edge`, and malformed `--param` loudly.
- **TASK-003.MI7:** Add Go unit tests for parsing/config precedence/help/error cases.

## TASK-003.P3 Done when

- **TASK-003.DW1:** `cd cli; go test ./...` passes.
- **TASK-003.DW2:** `go build -o ./cli/bin/todo ./cli/cmd/todo` produces a runnable binary.
- **TASK-003.DW3:** No Go code parses EDN or imports an EDN library.

## TASK-003.P4 Out of scope

- **TASK-003.OS1:** Connecting to the daemon socket for real task/query operations.
- **TASK-003.OS2:** Implementing daemon start process execution beyond command shape/stubs.

## TASK-003.P5 References

- **TASK-003.REF1:** `GOCLI-PLAN-001.PH3`, `GOCLI-PLAN-001.TC5`.
- **TASK-003.REF2:** `SPEC-002-D003.CC1`, `SPEC-002-D003.CC2`, `SPEC-002-D003.CC8` through `SPEC-002-D003.CC13`.
- **TASK-003.REF3:** `GOCLI-PROP-001.S4`, `GOCLI-PROP-001.S5`, `GOCLI-PROP-001.S8`.
