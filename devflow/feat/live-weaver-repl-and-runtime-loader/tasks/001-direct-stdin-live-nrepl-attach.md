# Task 1: Direct stdin live nREPL attach

**Document ID:** `TASK-LWRL-001`

## TASK-LWRL-001.P1 Scope

Type: AFK

Implement the first vertical slice of direct live weaver evaluation for `strand weaver repl --stdin`. The CLI may still launch a small Clojure attach process, but user forms must be sent to the selected running weaver nREPL and evaluated in the weaver JVM.

## TASK-LWRL-001.P2 Must implement exactly

- **TASK-LWRL-001.MI1:** Update `cli/internal/command/command.go` so `launchRepl` reads `nrepl.host` and `nrepl.port` from the `weaver-repl-context` response and passes them to the REPL process for `--stdin`; keep the existing fail-loud checks for malformed mill responses.
- **TASK-LWRL-001.MI2:** Replace the `--stdin` helper-JVM path built by `replArgs` with a thin attach-client invocation. The attach client may live in `src/skein/repl.clj` or a new namespace, but it must not call `skein.repl/connect!`, `skein.client/call-world`, or evaluate user forms locally.
- **TASK-LWRL-001.MI3:** The attach client must open an nREPL connection to the selected weaver endpoint, establish a session with `skein.repl` required and active as the evaluation namespace, evaluate each top-level stdin form in order, print one direct Clojure result per form, and exit non-zero on read, eval, or transport failure.
- **TASK-LWRL-001.MI4:** Preserve `skein.repl/connect!` and `skein.client/call-world` behavior for explicit client/test workflows; do not remove the fixed-form API bridge.
- **TASK-LWRL-001.MI5:** Update focused Go tests in `cli/internal/command/command_test.go` for the new attach-client args and malformed/missing nREPL metadata handling.
- **TASK-LWRL-001.MI6:** Add or update a Clojure/integration test that starts a disposable weaver and proves `strand weaver repl --stdin` evaluates inside the weaver JVM by reading `@skein.weaver.runtime/current-runtime` or another weaver-only runtime value.

## TASK-LWRL-001.P3 Done when

- **TASK-LWRL-001.DW1:** `printf '@skein.weaver.runtime/current-runtime\n' | strand --config-dir "$world" weaver repl --stdin` prints a non-nil runtime-shaped value for a disposable running weaver.
- **TASK-LWRL-001.DW2:** A form that throws in weaver evaluation causes `strand weaver repl --stdin` to exit non-zero and print an informative error.
- **TASK-LWRL-001.DW3:** Relevant tests for `command.go` and the new stdin attach behavior pass.

## TASK-LWRL-001.P4 Out of scope

- **TASK-LWRL-001.OS1:** Do not implement polished interactive terminal behavior beyond what is needed for stdin mode.
- **TASK-LWRL-001.OS2:** Do not rename `skein.libs.alpha` or change generated config templates in this task.
- **TASK-LWRL-001.OS3:** Do not add nREPL authentication, sandboxing, or remote security behavior.

## TASK-LWRL-001.P5 References

- **TASK-LWRL-001.REF1:** `devflow/feat/live-weaver-repl-and-runtime-loader/proposal.md` goals `LWRL-PROP-001.G1` through `LWRL-PROP-001.G3`.
- **TASK-LWRL-001.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/cli.delta.md` contracts `LWRL-DELTA-CLI-001.C1` through `LWRL-DELTA-CLI-001.C3`.
- **TASK-LWRL-001.REF3:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/repl-api.delta.md` contracts `LWRL-DELTA-REPL-001.C2` through `LWRL-DELTA-REPL-001.C4`.
- **TASK-LWRL-001.REF4:** Current implementation: `cli/internal/command/command.go` `launchRepl`, `replArgs`, and `runReplProcess`; `cli/cmd/mill/lifecycle.go` `weaverReplContext`; `src/skein/repl.clj` `-main`; `src/skein/client.clj` fixed-form bridge.
