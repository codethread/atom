# Task 6: Attach REPL using mill metadata

**Document ID:** `TASK-MillRouterRuntime-006`

## TASK-MillRouterRuntime-006.P1 Scope

Type: AFK

Update `strand weaver repl` and `strand weaver repl --stdin` so mill resolves and verifies the selected weaver, then the helper JVM attaches directly to nREPL using explicit XDG metadata/state references. This task is now an unblocker for closing Task 3 because full smoke validation exercises `strand weaver repl --stdin` after XDG/mill-routed weaver startup.

## TASK-MillRouterRuntime-006.P2 Must implement exactly

- **TASK-MillRouterRuntime-006.MI1:** Route `strand weaver repl` and `strand weaver repl --stdin` through mill for selected-world resolution and running-weaver verification before launching Clojure.
- **TASK-MillRouterRuntime-006.MI2:** Extend the helper launch arguments or environment so `skein.repl` receives both selected config identity and the XDG state/metadata reference needed to find the running weaver.
- **TASK-MillRouterRuntime-006.MI3:** Update `skein.client` metadata lookup to support explicit state/metadata references instead of assuming metadata lives below config-dir.
- **TASK-MillRouterRuntime-006.MI4:** Keep nREPL traffic direct between helper JVM and weaver; mill must not proxy interactive or stdin REPL bytes.
- **TASK-MillRouterRuntime-006.MI5:** Preserve identity verification using selected config-dir and weaver nonce/protocol.
- **TASK-MillRouterRuntime-006.MI6:** Keep `init!` as an idempotent trusted helper if existing tests or workflows need it, but remove docs/tests that imply normal CLI users must run it after startup.
- **TASK-MillRouterRuntime-006.MI7:** Resolve the stale/missing metadata failure observed after Task 4's XDG weaver startup path for `strand weaver repl --stdin`; this is REPL attachment scope, not storage startup scope.

## TASK-MillRouterRuntime-006.P3 Done when

- **TASK-MillRouterRuntime-006.DW1:** `strand weaver repl --stdin` can evaluate `(strands)` against a mill-started weaver in an isolated integration test.
- **TASK-MillRouterRuntime-006.DW2:** Tests prove `strand weaver repl` fails clearly when mill is unavailable or when no selected-world weaver is running.
- **TASK-MillRouterRuntime-006.DW3:** Clojure client tests cover metadata lookup with config-dir separate from state-dir.
- **TASK-MillRouterRuntime-006.DW4:** No Go code implements nREPL protocol parsing or byte tunneling.
- **TASK-MillRouterRuntime-006.DW5:** Relevant Go and Clojure tests pass: `(cd cli && go test ./...)` and `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test`.
- **TASK-MillRouterRuntime-006.DW6:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke` no longer fails at `strand weaver repl --stdin` with stale/missing weaver metadata; if a later unrelated smoke failure remains, record it in the plan Developer Notes with the failing command/output summary.

## TASK-MillRouterRuntime-006.P4 Out of scope

- **TASK-MillRouterRuntime-006.OS1:** New REPL helper functions beyond attachment changes.
- **TASK-MillRouterRuntime-006.OS2:** Mill-proxied REPL sessions.
- **TASK-MillRouterRuntime-006.OS3:** Editor integration beyond the existing plain helper REPL path.

## TASK-MillRouterRuntime-006.P5 References

- **TASK-MillRouterRuntime-006.REF1:** [REPL API delta](../specs/repl-api.delta.md)
- **TASK-MillRouterRuntime-006.REF2:** `src/skein/repl.clj`, `src/skein/client.clj`, `cli/internal/command`
