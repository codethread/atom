# Task 5: Forward strand operations through mill

**Document ID:** `TASK-MillRouterRuntime-005`

## TASK-MillRouterRuntime-005.P1 Scope

Type: AFK

Move normal public `strand` commands from direct weaver socket clients to mill-routed forwarding while keeping CLI parsing and weaver semantic behavior intact.

## TASK-MillRouterRuntime-005.P2 Must implement exactly

- **TASK-MillRouterRuntime-005.MI1:** Replace direct `strand -> weaver` calls for `add`, `update`, `show`, `supersede`, `burn`, `list`, `ready`, `weave`, `pattern explain`, and `op` with `strand -> mill` requests.
- **TASK-MillRouterRuntime-005.MI2:** Have mill resolve the selected world, verify a running weaver exists for that world, forward the existing operation name/payload to that weaver's JSON socket, and return the weaver response unchanged except for transport wrapping needed by strand.
- **TASK-MillRouterRuntime-005.MI3:** If mill is running but no selected-world weaver is running, ordinary commands fail non-zero with remediation to run `strand weaver start`.
- **TASK-MillRouterRuntime-005.MI4:** Preserve existing CLI parsing/validation for states, attributes, stdin JSON, pattern names, query params, and op passthrough before the request reaches mill.
- **TASK-MillRouterRuntime-005.MI5:** Preserve public JSON-only successful output shapes for weaver responses.
- **TASK-MillRouterRuntime-005.MI6:** Preserve structured non-zero behavior for weaver domain errors, protocol errors, stale selected-world metadata, and missing mill.
- **TASK-MillRouterRuntime-005.MI7:** Ensure public request payloads forwarded to the weaver do not include cwd, Git root, or raw `--config-dir`; those remain mill routing envelope data only.

## TASK-MillRouterRuntime-005.P3 Done when

- **TASK-MillRouterRuntime-005.DW1:** Go tests cover successful forwarding for at least `add`, `list`, `ready`, and `op help` through a fake or real mill/weaver boundary.
- **TASK-MillRouterRuntime-005.DW2:** Go tests cover no mill, no selected-world weaver, and weaver domain error propagation.
- **TASK-MillRouterRuntime-005.DW3:** Existing CLI attribute/stdin/pattern/op parsing tests still pass after transport replacement.
- **TASK-MillRouterRuntime-005.DW4:** An integration test with isolated XDG state can run `mill`, `strand init`, `strand weaver start`, `strand add "hello"`, and `strand list` successfully.
- **TASK-MillRouterRuntime-005.DW5:** `(cd cli && go test ./...)` passes.

## TASK-MillRouterRuntime-005.P4 Out of scope

- **TASK-MillRouterRuntime-005.OS1:** Connected REPL changes.
- **TASK-MillRouterRuntime-005.OS2:** Adding new user-facing strand commands.
- **TASK-MillRouterRuntime-005.OS3:** Ordinary-command weaver autostart.

## TASK-MillRouterRuntime-005.P5 References

- **TASK-MillRouterRuntime-005.REF1:** [CLI delta](../specs/cli.delta.md)
- **TASK-MillRouterRuntime-005.REF2:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md)
- **TASK-MillRouterRuntime-005.REF3:** `cli/internal/command`, `cli/internal/client`, `src/skein/weaver/socket.clj`
