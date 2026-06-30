# Task 1: Add mill command and XDG state root

**Document ID:** `TASK-MillRouterRuntime-001`

## TASK-MillRouterRuntime-001.P1 Scope

Type: AFK

Create the minimal Go `mill` command and shared state-root/world identity primitives needed by later tasks. This task does not start weavers or route strand operations beyond a health/status request.

## TASK-MillRouterRuntime-001.P2 Must implement exactly

- **TASK-MillRouterRuntime-001.MI1:** Add a Go `mill` command under the existing Go workspace, installed by `make install` alongside `strand`.
- **TASK-MillRouterRuntime-001.MI2:** Implement `mill start` as a foreground local process that creates the Skein XDG state root: `$XDG_STATE_HOME/skein` or `~/.local/state/skein` when `XDG_STATE_HOME` is unset.
- **TASK-MillRouterRuntime-001.MI3:** Have `mill start` publish `mill.json` and `mill.sock` under the state root with enough identity for clients to reject stale/mismatched metadata.
- **TASK-MillRouterRuntime-001.MI4:** Define shared Go helpers for canonical world identity and per-world runtime dirs under `weavers/<hash>`, where the hash is safe for filesystem/socket paths and derived from canonical config identity.
- **TASK-MillRouterRuntime-001.MI5:** Add a minimal mill request/response envelope and a `status` or `ping` operation used only to prove the client can reach the active mill.
- **TASK-MillRouterRuntime-001.MI6:** Add focused Go tests that use isolated temporary `XDG_STATE_HOME` values and leave no state under the real user home.

## TASK-MillRouterRuntime-001.P3 Done when

- **TASK-MillRouterRuntime-001.DW1:** `go install ./cli/cmd/mill` or the chosen command path succeeds through `make install`.
- **TASK-MillRouterRuntime-001.DW2:** A focused Go test can start a test mill, read its metadata, connect to its socket, and receive a successful health/status response.
- **TASK-MillRouterRuntime-001.DW3:** State-root and per-world path tests cover `XDG_STATE_HOME` set and unset fallback behavior without mutating the real fallback.
- **TASK-MillRouterRuntime-001.DW4:** `(cd cli && go test ./...)` passes for the touched Go packages.

## TASK-MillRouterRuntime-001.P4 Out of scope

- **TASK-MillRouterRuntime-001.OS1:** Starting Clojure weavers.
- **TASK-MillRouterRuntime-001.OS2:** Changing `strand` command behavior beyond shared helper extraction needed by tests.
- **TASK-MillRouterRuntime-001.OS3:** Public active-weaver listing or dashboards.

## TASK-MillRouterRuntime-001.P5 References

- **TASK-MillRouterRuntime-001.REF1:** [CLI delta](../specs/cli.delta.md)
- **TASK-MillRouterRuntime-001.REF2:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md)
- **TASK-MillRouterRuntime-001.REF3:** `Makefile`, `cli/cmd`, `cli/internal/config`, `cli/internal/client`
