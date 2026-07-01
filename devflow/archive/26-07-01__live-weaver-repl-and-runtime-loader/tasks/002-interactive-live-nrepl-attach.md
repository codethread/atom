# Task 2: Interactive live nREPL attach

**Document ID:** `TASK-LWRL-002`

## TASK-LWRL-002.P1 Scope

Type: AFK

Extend the direct attach path from stdin mode to the default interactive `strand weaver repl` command. Users should land in a useful weaver-side REPL where forms evaluate in the selected weaver JVM.

## TASK-LWRL-002.P2 Must implement exactly

- **TASK-LWRL-002.MI1:** Make default `strand weaver repl` use the same direct nREPL endpoint metadata and thin attach-client topology as `--stdin`; it must not start a helper JVM that evaluates user forms locally.
- **TASK-LWRL-002.MI2:** Start the live session in, or immediately switch it into, a useful weaver-side namespace where `(require 'skein.repl)` has succeeded and compact helper names are usable through the in-process dispatch from Task 3.
- **TASK-LWRL-002.MI3:** Keep mill's role limited to `weaver-repl-context`: selected-world resolution, liveness verification, nREPL metadata, and source resolution for launching the attach client. Do not proxy nREPL traffic through mill.
- **TASK-LWRL-002.MI4:** Update CLI help text that currently says "connected Clojure helper REPL" so it describes direct live weaver attachment without implying sandboxing or a separate semantic runtime.
- **TASK-LWRL-002.MI5:** Update Go command tests so default repl launch arguments and source use reflect the thin attach-client path, while stopped/non-running weaver failures remain fail-loud before source-dependent attach work proceeds.
- **TASK-LWRL-002.MI6:** Add a deterministic automated proof for the non-stdin attach path against a disposable running weaver. The proof may drive the attach client with scripted input, factor and exercise the interactive session-initialization path directly, or use another non-flaky harness, but it must prove the non-stdin path opens a live weaver nREPL session rather than only asserting command-line arguments.

## TASK-LWRL-002.P3 Done when

- **TASK-LWRL-002.DW1:** `strand --config-dir "$world" weaver repl` connects to the selected running weaver nREPL rather than to a local helper eval loop, and the automated proof exercises the non-stdin live session path.
- **TASK-LWRL-002.DW2:** The launched attach client receives nREPL host/port from mill metadata and no longer depends on config-dir/state-dir as the semantic target for interactive evaluation.
- **TASK-LWRL-002.DW3:** Relevant Go command tests pass and no default REPL test still asserts helper-JVM `skein.repl [config-dir] [state-dir]` semantics.

## TASK-LWRL-002.P4 Out of scope

- **TASK-LWRL-002.OS1:** Do not add an explicit `--client` helper mode unless an existing test cannot be preserved any other way; if added, it must be opt-in and clearly named.
- **TASK-LWRL-002.OS2:** Do not broaden CLI commands beyond `weaver repl` and `weaver repl --stdin`.
- **TASK-LWRL-002.OS3:** Do not perform docs/spec promotion; feature-local deltas remain the contract source until finish/archive.

## TASK-LWRL-002.P5 References

- **TASK-LWRL-002.REF1:** Depends on `TASK-LWRL-001` for the direct nREPL attach-client foundation and `TASK-LWRL-003` for helper-ready in-process dispatch.
- **TASK-LWRL-002.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/cli.delta.md` contracts `LWRL-DELTA-CLI-001.C1`, `LWRL-DELTA-CLI-001.C3`, and `LWRL-DELTA-CLI-001.C4`.
- **TASK-LWRL-002.REF3:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/repl-api.delta.md` contracts `LWRL-DELTA-REPL-001.C2`, `LWRL-DELTA-REPL-001.C3`, and `LWRL-DELTA-REPL-001.C5`.
- **TASK-LWRL-002.REF4:** Current implementation: `cli/internal/command/command.go` `rootCommand` repl subcommand, `launchRepl`, `replArgs`; `cli/cmd/mill/lifecycle.go` `weaverReplContext`.
