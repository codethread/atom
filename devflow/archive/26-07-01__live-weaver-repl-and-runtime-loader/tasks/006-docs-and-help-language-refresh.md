# Task 6: Docs and help language refresh

**Document ID:** `TASK-LWRL-006`

## TASK-LWRL-006.P1 Scope

Type: AFK

Refresh user-facing language after direct live REPL attach, in-process helpers, runtime-loader namespace split, and library cleanup have landed. Docs and help should distinguish live weaver authority, privileged built-in helpers, and ordinary user/community libraries.

## TASK-LWRL-006.P2 Must implement exactly

- **TASK-LWRL-006.MI1:** Update CLI help strings or generated messages still describing the default REPL as a connected helper JVM or ordinary library/package workflow.
- **TASK-LWRL-006.MI2:** Update `README.md`, `docs/getting-started.md`, `docs/skein.md`, and any directly affected inline examples to describe `strand weaver repl` as live weaver attachment.
- **TASK-LWRL-006.MI3:** Update examples in docs and comments from `skein.libs.alpha` to `skein.runtime.alpha` except where explicitly documenting alpha compatibility.
- **TASK-LWRL-006.MI4:** Document `skein.runtime.alpha` and other shipped `skein.*.alpha` namespaces as privileged built-in helper namespaces, not as proof that ordinary user libraries can implement equivalent loader/runtime behavior.
- **TASK-LWRL-006.MI5:** Keep user/community libraries described as trusted Clojure loaded through config, approved roots, or direct live REPL experimentation; do not introduce package or plugin command language.
- **TASK-LWRL-006.MI6:** Keep feature-local deltas as planned staging docs; do not promote them into root specs in this task.

## TASK-LWRL-006.P3 Done when

- **TASK-LWRL-006.DW1:** Searching docs, help strings, and comments for `skein.libs.alpha`, `connected helper REPL`, and similar old-story language finds only intentional compatibility/history references.
- **TASK-LWRL-006.DW2:** Docs consistently present direct REPL, built-in privileged helpers, and user/community libraries according to the feature deltas.
- **TASK-LWRL-006.DW3:** Relevant docs-sensitive tests, Go help tests, and smoke examples pass.

## TASK-LWRL-006.P4 Out of scope

- **TASK-LWRL-006.OS1:** Do not archive the feature or merge feature-local spec deltas into root specs; finish/archive owns that.
- **TASK-LWRL-006.OS2:** Do not remove `skein.libs.alpha` compatibility.
- **TASK-LWRL-006.OS3:** Do not change runtime/library implementation except for copy-test fallout directly required by the docs/help updates.

## TASK-LWRL-006.P5 References

- **TASK-LWRL-006.REF1:** `devflow/feat/live-weaver-repl-and-runtime-loader/proposal.md` goals `LWRL-PROP-001.G4` through `LWRL-PROP-001.G6`.
- **TASK-LWRL-006.REF2:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/cli.delta.md` contracts `LWRL-DELTA-CLI-001.C5` and `LWRL-DELTA-CLI-001.C6`.
- **TASK-LWRL-006.REF3:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/repl-api.delta.md` contract `LWRL-DELTA-REPL-001.C9`.
- **TASK-LWRL-006.REF4:** `devflow/feat/live-weaver-repl-and-runtime-loader/specs/daemon-runtime.delta.md` contracts `LWRL-DELTA-RUNTIME-001.C7` and `LWRL-DELTA-RUNTIME-001.C8`.
- **TASK-LWRL-006.REF5:** Docs to inspect include `README.md`, `docs/getting-started.md`, `docs/skein.md`, generated config examples, CLI help text in `cli/internal/command/command.go`, and smoke embedded examples in `dev/skein/smoke.clj`.
