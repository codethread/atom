# Task 6: Update docs and promote specs

**Document ID:** `TASK-RepoFirstConfig-006`

## TASK-RepoFirstConfig-006.P1 Scope

Type: AFK

Update user-facing docs and root specs for the shipped repo-first config behavior after implementation and validation are complete.

## TASK-RepoFirstConfig-006.P2 Must implement exactly

- **TASK-RepoFirstConfig-006.MI1:** Merge `devflow/feat/repo-first-config/specs/cli.delta.md` into `devflow/specs/cli.md`, replacing old XDG implicit-default/no-cwd-switching contracts with repo-first default discovery and explicit `--config-dir` override behavior.
- **TASK-RepoFirstConfig-006.MI2:** Merge `devflow/feat/repo-first-config/specs/daemon-runtime.delta.md` into `devflow/specs/daemon-runtime.md`, including layered init, layered libs, local override, and event-safe reload semantics.
- **TASK-RepoFirstConfig-006.MI3:** Merge `devflow/feat/repo-first-config/specs/repl-api.delta.md` into `devflow/specs/repl-api.md`, including effective approved libs and reload/startup parity.
- **TASK-RepoFirstConfig-006.MI4:** Update `README.md`, `docs/getting-started.md`, and any relevant devflow/project guidance that still describes default global config worlds as the ordinary path.
- **TASK-RepoFirstConfig-006.MI5:** Include concise examples for `.skein` layout, committed vs gitignored files, `strand init --source`, `SKEIN_SOURCE`, `init.local.clj`, and `libs.local.edn` personal workflow libraries.
- **TASK-RepoFirstConfig-006.MI6:** Mark feature-local deltas `Merged` only after root specs are updated and implementation validation from task 4 is complete.
- **TASK-RepoFirstConfig-006.MI7:** Update `devflow/README.md` active/archive status if the feature is ready for finish/archive, but do not move the feature folder unless the user explicitly asks for finish/archive.

## TASK-RepoFirstConfig-006.P3 Done when

- **TASK-RepoFirstConfig-006.DW1:** Root specs describe current shipped repo-first contracts with no contradictory XDG default-world language.
- **TASK-RepoFirstConfig-006.DW2:** User docs show the personal global-workflow-lib use case through `libs.local.edn` and `init.local.clj`.
- **TASK-RepoFirstConfig-006.DW3:** Feature-local deltas are marked `Merged` and root spec dates/statuses are updated.
- **TASK-RepoFirstConfig-006.DW4:** Primary validation from task 4 remains passing after docs/spec edits where applicable.

## TASK-RepoFirstConfig-006.P4 Out of scope

- **TASK-RepoFirstConfig-006.OS1:** Do not archive the feature folder unless running the separate devflow finish/archive procedure.
- **TASK-RepoFirstConfig-006.OS2:** Do not add package management or source installation docs beyond explicitly saying they are out of scope for this MVP.

## TASK-RepoFirstConfig-006.P5 References

- **TASK-RepoFirstConfig-006.REF1:** Feature deltas in `devflow/feat/repo-first-config/specs/`.
- **TASK-RepoFirstConfig-006.REF2:** Root specs: `devflow/specs/cli.md`, `devflow/specs/daemon-runtime.md`, `devflow/specs/repl-api.md`.
- **TASK-RepoFirstConfig-006.REF3:** User docs: `README.md`, `docs/getting-started.md`, `AGENTS.md` if project operation guidance changes.
