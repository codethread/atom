# Task 7: Update smoke docs and specs

**Document ID:** `TASK-MillRouterRuntime-007`

## TASK-MillRouterRuntime-007.P1 Scope

Type: AFK

Complete validation and user-facing alignment for the mill-routed runtime model. Promote durable spec changes only after implementation matches the feature deltas.

## TASK-MillRouterRuntime-007.P2 Must implement exactly

- **TASK-MillRouterRuntime-007.MI1:** Update smoke tests to start an isolated `mill` with disposable XDG state, initialize a Git repo world, start a weaver, exercise CLI strand commands, exercise `weaver repl --stdin`, stop the weaver, and stop/clean mill state.
- **TASK-MillRouterRuntime-007.MI2:** Update `README.md`, `docs/getting-started.md`, and relevant user reference docs to show the new flow: install `mill`/`strand`, start `mill`, `strand init` in a Git repo, `strand weaver start`, then strand commands.
- **TASK-MillRouterRuntime-007.MI3:** Remove docs that tell users to run CLI DB init after starting the weaver. If `init!` remains documented, describe it as a trusted REPL helper, not normal setup.
- **TASK-MillRouterRuntime-007.MI4:** Update `Makefile` targets so install/bootstrap behavior matches the mill model and does not run `git init` or call weaver DB init.
- **TASK-MillRouterRuntime-007.MI5:** Merge the feature-local CLI, Weaver Runtime, and REPL API deltas into root specs after implementation is complete; mark deltas `Merged`.
- **TASK-MillRouterRuntime-007.MI6:** Update `devflow/README.md` active/archive status only if finishing/archive is part of the same implementation pass.
- **TASK-MillRouterRuntime-007.MI7:** Verify validation leaves no generated SQLite, sockets, or runtime metadata in the repo working tree.

## TASK-MillRouterRuntime-007.P3 Done when

- **TASK-MillRouterRuntime-007.DW1:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes.
- **TASK-MillRouterRuntime-007.DW2:** `(cd cli && go test ./...)` passes.
- **TASK-MillRouterRuntime-007.DW3:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke` passes.
- **TASK-MillRouterRuntime-007.DW4:** README/getting-started examples no longer show the old direct weaver or CLI DB init sequence.
- **TASK-MillRouterRuntime-007.DW5:** `git status --short` shows only intentional source/doc/devflow changes, not runtime artifacts.

## TASK-MillRouterRuntime-007.P4 Out of scope

- **TASK-MillRouterRuntime-007.OS1:** Adding optional global personal worlds or named world references.
- **TASK-MillRouterRuntime-007.OS2:** Active-weaver listing dashboards.
- **TASK-MillRouterRuntime-007.OS3:** Archiving the feature unless all implementation tasks are complete and the owner explicitly proceeds with finish/archive.

## TASK-MillRouterRuntime-007.P5 References

- **TASK-MillRouterRuntime-007.REF1:** [CLI delta](../specs/cli.delta.md)
- **TASK-MillRouterRuntime-007.REF2:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md)
- **TASK-MillRouterRuntime-007.REF3:** [REPL API delta](../specs/repl-api.delta.md)
- **TASK-MillRouterRuntime-007.REF4:** `README.md`, `docs/getting-started.md`, `Makefile`, `dev/skein/smoke.clj`
