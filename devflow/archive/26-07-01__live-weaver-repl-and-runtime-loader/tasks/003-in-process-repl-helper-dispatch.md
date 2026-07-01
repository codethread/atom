# Task 3: In-process repl helper dispatch

**Document ID:** `TASK-LWRL-003`

## TASK-LWRL-003.P1 Scope

Type: AFK

Make `skein.repl` helper functions work naturally when evaluated inside the weaver JVM by dispatching through `@skein.weaver.runtime/current-runtime`, while preserving explicit connected-client behavior for tests and lower-level Clojure clients.

## TASK-LWRL-003.P2 Must implement exactly

- **TASK-LWRL-003.MI1:** Update `src/skein/repl.clj` so helper calls first detect `@skein.weaver.runtime/current-runtime` and call the matching `skein.weaver.api` operation directly with that runtime.
- **TASK-LWRL-003.MI2:** Preserve the existing connected fallback through `skein.client/call-world`, `connected-config-dir`, and `connected-opts` for `connect!` workflows when no in-process runtime is present.
- **TASK-LWRL-003.MI3:** Apply the in-process dispatch consistently to core helpers including `init!`, `strand!`, `update!`, `supersede!`, `strand`, relation helpers, burn helpers, query registration/loading/listing, ad hoc and named query execution, `strands`, `ready`, and pattern/weave helpers where the called alpha namespace already has in-process support.
- **TASK-LWRL-003.MI4:** Keep existing weaver error translation behavior from `call-daemon` so user-facing exceptions remain informative in both in-process and connected modes.
- **TASK-LWRL-003.MI5:** Add Clojure tests in `test/skein/repl_test.clj` or a focused companion test that bind/start a disposable runtime, call `skein.repl` helpers without `connect!`, and assert real mutation/query behavior succeeds through `current-runtime`.
- **TASK-LWRL-003.MI6:** Preserve or update existing connected-client tests so `connect!` remains valid for explicit client/test workflows.

## TASK-LWRL-003.P3 Done when

- **TASK-LWRL-003.DW1:** Inside a running weaver process, `(skein.repl/strand! "x")` and `(skein.repl/strands)` work without prior `(connect! ...)`.
- **TASK-LWRL-003.DW2:** Outside a weaver process, helper calls still fail loudly until `(connect! config-dir state-dir)` selects a running world.
- **TASK-LWRL-003.DW3:** Relevant Clojure tests for in-process and connected helper dispatch pass.

## TASK-LWRL-003.P4 Out of scope

- **TASK-LWRL-003.OS1:** Do not implement or change nREPL attach transport in this task.
- **TASK-LWRL-003.OS2:** Do not rename runtime loader namespaces in this task.
- **TASK-LWRL-003.OS3:** Do not bypass `skein.weaver.api` by directly mutating database or runtime internals from helpers.

## TASK-LWRL-003.P5 References

- **TASK-LWRL-003.REF1:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/repl-api.delta.md` contract `LWRL-DELTA-REPL-001.C6`.
- **TASK-LWRL-003.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/daemon-runtime.delta.md` contract `LWRL-DELTA-RUNTIME-001.C3`.
- **TASK-LWRL-003.REF3:** Existing in-process dispatch pattern: `src/skein/libs/alpha.clj` and `src/skein/*/alpha.clj` helper namespaces.
- **TASK-LWRL-003.REF4:** Current helper implementation: `src/skein/repl.clj`; fixed bridge to preserve: `src/skein/client.clj` `call-world` and `fixed-form`.
