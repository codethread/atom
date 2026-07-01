# Task 5: Library namespace cleanup proof

**Document ID:** `TASK-LWRL-005`

## TASK-LWRL-005.P1 Scope

Type: AFK

Clean up code under library/example naming after the runtime-loader rename. Anything remaining under `src/skein/libs` must be either authorable userland-style code or the explicit `skein.libs.alpha` compatibility shim.

## TASK-LWRL-005.P2 Must implement exactly

- **TASK-LWRL-005.MI1:** Audit `src/skein/libs/*`. Leave `skein.libs.alpha` only as the compatibility shim from Task 4, keep `skein.libs.ephemeral` only if it remains userland-style, and move or rewrite any privileged loader/runtime behavior found under library naming.
- **TASK-LWRL-005.MI2:** Add or update a focused test proving at least one shipped library/example under `src/skein/libs` composes documented helper/API surfaces and does not proxy privileged loader/config internals.
- **TASK-LWRL-005.MI3:** If new non-shim code remains under `src/skein/libs`, ensure it can be explained as code a user could author through trusted config, approved roots, or direct live REPL evaluation.
- **TASK-LWRL-005.MI4:** Update nearby namespace docstrings or test names only as needed to make the code boundary explicit; broad docs/help copy belongs to Task 6.

## TASK-LWRL-005.P3 Done when

- **TASK-LWRL-005.DW1:** `src/skein/libs` contains no privileged loader/config implementation except the explicit `skein.libs.alpha` compatibility shim.
- **TASK-LWRL-005.DW2:** A focused test or smoke example proves the remaining shipped library/example style is authorable userland-style code using documented helpers/APIs.
- **TASK-LWRL-005.DW3:** Relevant Clojure tests for library namespaces and compatibility pass.

## TASK-LWRL-005.P4 Out of scope

- **TASK-LWRL-005.OS1:** Do not update broad product docs, CLI help text, or root specs in this task.
- **TASK-LWRL-005.OS2:** Do not remove alpha compatibility for `skein.libs.alpha`.
- **TASK-LWRL-005.OS3:** Do not create package registry, dependency solver, source fetcher, or plugin commands.

## TASK-LWRL-005.P5 References

- **TASK-LWRL-005.REF1:** `devflow/feat/live-weaver-repl-and-runtime-loader/proposal.md` goal `LWRL-PROP-001.G7` and scope `LWRL-PROP-001.S6`.
- **TASK-LWRL-005.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/repl-api.delta.md` contract `LWRL-DELTA-REPL-001.C10`.
- **TASK-LWRL-005.REF3:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/daemon-runtime.delta.md` contract `LWRL-DELTA-RUNTIME-001.C9`.
- **TASK-LWRL-005.REF4:** Desired userland-style example: `src/skein/libs/ephemeral.clj`; compatibility shim target: `src/skein/libs/alpha.clj`; new privileged namespace from Task 4: `src/skein/runtime/alpha.clj`.
