# Task 3: Start weavers through mill

**Document ID:** `TASK-MillRouterRuntime-003`

## TASK-MillRouterRuntime-003.P1 Scope

Type: AFK

Implement mill-owned weaver lifecycle for `strand weaver start/status/stop` at the Go process boundary. This task depends on the Clojure runtime already accepting explicit config/state/data dirs, and must launch weavers using that contract.

## TASK-MillRouterRuntime-003.P2 Must implement exactly

- **TASK-MillRouterRuntime-003.MI1:** Route `strand weaver start`, `strand weaver status`, and `strand weaver stop` through mill.
- **TASK-MillRouterRuntime-003.MI2:** Have mill resolve the selected world from cwd or explicit `--config-dir` and compute that world's XDG state/data dirs.
- **TASK-MillRouterRuntime-003.MI3:** Start at most one child weaver per canonical selected world. Repeated `strand weaver start` should fail loudly or return already-running JSON after verifying identity.
- **TASK-MillRouterRuntime-003.MI4:** Launch Clojure with selected config-dir plus explicit state/data dir arguments required by the new runtime contract.
- **TASK-MillRouterRuntime-003.MI5:** Track child process handles in mill memory and stop the selected child on `strand weaver stop`.
- **TASK-MillRouterRuntime-003.MI6:** Make `strand weaver status` return JSON from mill that distinguishes no selected-world weaver, starting/running/stopped, and stale metadata where applicable.
- **TASK-MillRouterRuntime-003.MI7:** Include the selected config identity, selected state dir, selected data dir, database path, pid, weaver identity, socket endpoint, and nREPL endpoint in running status responses.
- **TASK-MillRouterRuntime-003.MI8:** Ensure `pkill mill` or abrupt mill process termination does not intentionally leave supervised child weavers running on supported local platforms.

## TASK-MillRouterRuntime-003.P3 Done when

- **TASK-MillRouterRuntime-003.DW1:** Go tests cover lifecycle routing with a fake weaver process/launcher and isolated XDG state.
- **TASK-MillRouterRuntime-003.DW2:** Starting two weavers for the same world is rejected or reported idempotently with identity verification.
- **TASK-MillRouterRuntime-003.DW3:** Starting weavers for two different Git repos computes two distinct per-world runtime dirs.
- **TASK-MillRouterRuntime-003.DW4:** `strand weaver stop` stops only the selected world's child in tests.
- **TASK-MillRouterRuntime-003.DW5:** `strand weaver status` tests assert the required running-status identity/path/endpoint fields.
- **TASK-MillRouterRuntime-003.DW6:** `(cd cli && go test ./...)` passes.

## TASK-MillRouterRuntime-003.P4 Out of scope

- **TASK-MillRouterRuntime-003.OS1:** Making ordinary `strand add/list` forward through mill.
- **TASK-MillRouterRuntime-003.OS2:** Clojure runtime argument parsing or schema initialization behavior; that is owned by Task 4.
- **TASK-MillRouterRuntime-003.OS3:** Active-weaver listing commands.

## TASK-MillRouterRuntime-003.P5 References

- **TASK-MillRouterRuntime-003.REF1:** [CLI delta](../specs/cli.delta.md)
- **TASK-MillRouterRuntime-003.REF2:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md)
- **TASK-MillRouterRuntime-003.REF3:** `cli/internal/command`, `cli/internal/client`, `src/skein/weaver/runtime.clj`
