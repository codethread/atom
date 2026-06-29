# Task 3: Implement layered init reload semantics

**Document ID:** `TASK-RepoFirstConfig-003`

## TASK-RepoFirstConfig-003.P1 Scope

Type: AFK

Implement weaver-owned `init.clj` plus `init.local.clj` layering for startup and config reload, including event-system-safe reload ordering.

## TASK-RepoFirstConfig-003.P2 Must implement exactly

- **TASK-RepoFirstConfig-003.MI1:** In `src/skein/weaver/runtime.clj`, replace single `init.clj` loading with ordered selected-config-dir startup file loading: `init.clj`, then `init.local.clj`.
- **TASK-RepoFirstConfig-003.MI2:** Missing startup files must be skipped; present files that fail to read/evaluate must abort startup loudly with file path context and publish no ready metadata.
- **TASK-RepoFirstConfig-003.MI3:** In `src/skein/weaver/api.clj`, update `reload-config!` so reload clears the same runtime state as today, reloads both init files in the same order as startup, and returns data that identifies loaded files and final return values.
- **TASK-RepoFirstConfig-003.MI4:** Preserve landed event-system semantics from `devflow/archive/26-06-27__weaver-event-system/`: reload clears event handlers, queued events, and recent failures; avoid an observable shared-only handler state between `init.clj` and `init.local.clj`.
- **TASK-RepoFirstConfig-003.MI5:** Preserve lifecycle hook, op registry, query, view, pattern, approved-lib sync, and module-use clearing behavior around reload.
- **TASK-RepoFirstConfig-003.MI6:** Add Clojure tests for startup load order, missing-local skip, failing-local startup failure, reload load order, reload missing-both behavior if the new contract permits no init files, and event/hook registry state after layered reload.

## TASK-RepoFirstConfig-003.P3 Done when

- **TASK-RepoFirstConfig-003.DW1:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes for touched Clojure tests.
- **TASK-RepoFirstConfig-003.DW2:** A weaver world with both init files observes shared effects before local effects on startup and reload.
- **TASK-RepoFirstConfig-003.DW3:** A failing present `init.local.clj` fails loudly during startup/reload rather than being ignored.
- **TASK-RepoFirstConfig-003.DW4:** Event handlers registered before reload do not survive unless reinstalled by the layered config.

## TASK-RepoFirstConfig-003.P4 Out of scope

- **TASK-RepoFirstConfig-003.OS1:** Do not implement `libs.local.edn` overlay; task 3 owns approved library merging.
- **TASK-RepoFirstConfig-003.OS2:** Do not change strand mutation/event payload semantics.

## TASK-RepoFirstConfig-003.P5 References

- **TASK-RepoFirstConfig-003.REF1:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md), [REPL API delta](../specs/repl-api.delta.md), [Plan](../repo-first-config.plan.md).
- **TASK-RepoFirstConfig-003.REF2:** Current anchors: `src/skein/weaver/runtime.clj`, `src/skein/weaver/api.clj`, `test/skein/libs_test.clj`, `test/skein/weaver_test.clj`.
- **TASK-RepoFirstConfig-003.REF3:** Event reload context: `devflow/archive/26-06-27__weaver-event-system/specs/daemon-runtime.delta.md` and current root `devflow/specs/daemon-runtime.md` event sections.
