# Task 7: End-to-end validation cleanup

**Document ID:** `TASK-LWRL-007`

## TASK-LWRL-007.P1 Scope

Type: AFK

Close the implementation loop with end-to-end validation across Clojure tests, Go CLI tests, smoke coverage, and generated artifact cleanup. This task should make the feature ready for owner review and later devflow finish/archive.

## TASK-LWRL-007.P2 Must implement exactly

- **TASK-LWRL-007.MI1:** Run the full primary validation suite from `AGENTS.md`: `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test`, `(cd cli && go test ./...)`, and `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke`.
- **TASK-LWRL-007.MI2:** If validation fails, make minimal fixes within the feature scope so direct live REPL attach, in-process helpers, runtime loader naming, and docs/examples agree.
- **TASK-LWRL-007.MI3:** Ensure smoke coverage includes direct live `strand weaver repl --stdin`, helper availability inside the weaver process, `@skein.weaver.runtime/current-runtime` introspection, fresh config using `skein.runtime.alpha`, and `skein.libs.alpha` compatibility.
- **TASK-LWRL-007.MI4:** Remove or update obsolete tests that still prove the default REPL is a local helper JVM, without weakening explicit `skein.client/call-world` fixed-form bridge coverage.
- **TASK-LWRL-007.MI5:** Check `git status --short` after validation and remove generated SQLite, runtime metadata, sockets, temporary worlds, or built CLI artifacts that should not be committed.
- **TASK-LWRL-007.MI6:** Append a concise Developer Notes entry to `devflow/feat/live-weaver-repl-and-runtime-loader/live-weaver-repl-and-runtime-loader.plan.md` summarizing validation results, any cut scope, and any follow-up that should be handled before finish/archive.

## TASK-LWRL-007.P3 Done when

- **TASK-LWRL-007.DW1:** The primary Clojure test suite passes.
- **TASK-LWRL-007.DW2:** The Go CLI test suite passes.
- **TASK-LWRL-007.DW3:** The smoke suite passes and proves the live REPL topology intended by the feature deltas.
- **TASK-LWRL-007.DW4:** `git status --short` shows only intentional source, docs, spec, or devflow changes.

## TASK-LWRL-007.P4 Out of scope

- **TASK-LWRL-007.OS1:** Do not merge feature-local spec deltas into root specs or archive the feature; that is a separate devflow finish/archive step after owner review.
- **TASK-LWRL-007.OS2:** Do not start a user-owned default weaver world; use disposable `--config-dir` worlds for validation.
- **TASK-LWRL-007.OS3:** Do not broaden the MVP with package commands, remote nREPL hardening, or removal of `skein.libs.alpha` compatibility.

## TASK-LWRL-007.P5 References

- **TASK-LWRL-007.REF1:** `AGENTS.md` validation and disposable-world guidance.
- **TASK-LWRL-007.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/live-weaver-repl-and-runtime-loader.plan.md` validation strategy `LWRL-PLAN-001.V1` through `LWRL-PLAN-001.V7`.
- **TASK-LWRL-007.REF3:** Integration coverage files: `dev/skein/smoke.clj`, `test/skein/repl_test.clj`, `test/skein/libs_test.clj`, `test/skein/client_test.clj`, `cli/internal/command/command_test.go`, and `cli/cmd/mill/lifecycle_test.go`.
