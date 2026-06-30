# Task 4: Runtime loader namespace split

**Document ID:** `TASK-LWRL-004`

## TASK-LWRL-004.P1 Scope

Type: AFK

Introduce `skein.runtime.alpha` as the privileged built-in runtime loader/config helper namespace, convert `skein.libs.alpha` into a compatibility shim, and update newly generated config to use the honest runtime namespace.

## TASK-LWRL-004.P2 Must implement exactly

- **TASK-LWRL-004.MI1:** Add `src/skein/runtime/alpha.clj` exposing the current loader/config helper API: `approved`, `sync!`, `syncs`, `reload!`, `use!`, `uses`, and `use`.
- **TASK-LWRL-004.MI2:** Implement `skein.runtime.alpha` with the same two-branch in-process/current-runtime and connected-client dispatch semantics used by the current `skein.libs.alpha`.
- **TASK-LWRL-004.MI3:** Convert `src/skein/libs/alpha.clj` into a thin alpha compatibility shim over `skein.runtime.alpha`; it must not own privileged loader/config behavior after this task.
- **TASK-LWRL-004.MI4:** Update `cli/internal/config/bootstrap.go` `DefaultInitCLJ` and any generated config tests so fresh `init.clj` uses `(require '[skein.runtime.alpha :as runtime])` and `(runtime/sync!)`.
- **TASK-LWRL-004.MI5:** Add or update tests so `skein.runtime.alpha` has parity for approved-root inspection, sync state, config reload, `use!`, `uses`, and `use`, while existing `skein.libs.alpha` calls remain compatible.
- **TASK-LWRL-004.MI6:** Update smoke assertions that inspect generated config templates to expect the new namespace while preserving compatibility smoke coverage for existing `skein.libs.alpha` where useful.

## TASK-LWRL-004.P3 Done when

- **TASK-LWRL-004.DW1:** Fresh `strand init` creates an `init.clj` requiring `skein.runtime.alpha` and calling `runtime/sync!`.
- **TASK-LWRL-004.DW2:** `(require '[skein.runtime.alpha :as runtime])` supports the full loader/config helper surface in both in-process and connected-client contexts.
- **TASK-LWRL-004.DW3:** `(require '[skein.libs.alpha :as libs])` still works for the same functions as a compatibility shim.
- **TASK-LWRL-004.DW4:** Relevant Clojure, Go config, and smoke-template tests pass.

## TASK-LWRL-004.P4 Out of scope

- **TASK-LWRL-004.OS1:** Do not rename `libs.edn`, `libs.local.edn`, or selected config-dir `libs/` directories.
- **TASK-LWRL-004.OS2:** Do not remove `skein.libs.alpha` compatibility in this feature.
- **TASK-LWRL-004.OS3:** Do not add package installation, dependency solving, or source fetching commands.

## TASK-LWRL-004.P5 References

- **TASK-LWRL-004.REF1:** `devflow/feat/live-weaver-repl-and-runtime-loader/proposal.md` scope `LWRL-PROP-001.S4` and `LWRL-PROP-001.S5`.
- **TASK-LWRL-004.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/repl-api.delta.md` contracts `LWRL-DELTA-REPL-001.C7` and `LWRL-DELTA-REPL-001.C8`.
- **TASK-LWRL-004.REF3:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/daemon-runtime.delta.md` contracts `LWRL-DELTA-RUNTIME-001.C5` and `LWRL-DELTA-RUNTIME-001.C6`.
- **TASK-LWRL-004.REF4:** Current implementation: `src/skein/libs/alpha.clj`, `src/skein/weaver/api.clj` loader operations, `cli/internal/config/bootstrap.go`, `test/skein/libs_test.clj`, and `dev/skein/smoke.clj` generated-config assertions.
