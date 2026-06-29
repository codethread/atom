# Task 2: Align Clojure world selection

**Document ID:** `TASK-RepoFirstConfig-002`

## TASK-RepoFirstConfig-002.P1 Scope

Type: AFK

Align Clojure-side world resolution and connected helper behavior with the repo-first selected world produced by the CLI slice.

## TASK-RepoFirstConfig-002.P2 Must implement exactly

- **TASK-RepoFirstConfig-002.MI1:** Update `src/skein/weaver/config.clj` so explicit config-dir construction remains available, while no-arg/default selected-world behavior no longer silently points at XDG global state for ordinary repo-first use.
- **TASK-RepoFirstConfig-002.MI2:** Update `src/skein/repl.clj` so `strand weaver repl` and `strand weaver repl --stdin` launched by the CLI reliably connect to the selected config-dir passed by the Go process.
- **TASK-RepoFirstConfig-002.MI3:** Preserve an explicit way for Clojure tests/helpers to construct disposable worlds without cwd discovery, matching the `--config-dir` test escape hatch.
- **TASK-RepoFirstConfig-002.MI4:** Reconcile no-arg `connect!` documentation and behavior with the new contract. If no-arg `connect!` remains for standalone use, it must not undermine CLI-launched repo selection; if it becomes fail-loud without context, update specs/tests accordingly.
- **TASK-RepoFirstConfig-002.MI5:** Add/update Clojure tests for explicit world construction, CLI-passed config-dir connection, and no-arg helper behavior under the chosen contract.
- **TASK-RepoFirstConfig-002.MI6:** Ensure Clojure metadata/client calls still discover by selected config-dir/state world and do not use database paths or repo metadata.

## TASK-RepoFirstConfig-002.P3 Done when

- **TASK-RepoFirstConfig-002.DW1:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes for touched Clojure tests.
- **TASK-RepoFirstConfig-002.DW2:** A CLI-launched helper REPL connects to the same `.skein` selected by repo discovery.
- **TASK-RepoFirstConfig-002.DW3:** Clojure disposable-world tests still have a deterministic explicit world construction path.

## TASK-RepoFirstConfig-002.P4 Out of scope

- **TASK-RepoFirstConfig-002.OS1:** Do not implement layered init or library overrides; tasks 3 and 4 own those runtime config semantics.
- **TASK-RepoFirstConfig-002.OS2:** Do not change public strand/query helper semantics beyond selected-world behavior.

## TASK-RepoFirstConfig-002.P5 References

- **TASK-RepoFirstConfig-002.REF1:** [REPL API delta](../specs/repl-api.delta.md), [CLI delta](../specs/cli.delta.md), [Plan PH3](../repo-first-config.plan.md#PLAN-RepoFirstConfig-001.PH3-Clojure-world-resolution-alignment).
- **TASK-RepoFirstConfig-002.REF2:** Current anchors: `src/skein/weaver/config.clj`, `src/skein/repl.clj`, `src/skein/client.clj`, `test/skein/weaver_test.clj`, `test/skein/repl_test.clj`.
