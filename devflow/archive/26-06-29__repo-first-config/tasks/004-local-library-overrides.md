# Task 4: Implement local library overrides

**Document ID:** `TASK-RepoFirstConfig-004`

## TASK-RepoFirstConfig-004.P1 Scope

Type: AFK

Implement effective approved-library config as `libs.edn` overlaid by `libs.local.edn`, with local entries replacing shared entries by coordinate.

## TASK-RepoFirstConfig-004.P2 Must implement exactly

- **TASK-RepoFirstConfig-004.MI1:** In `src/skein/weaver/api.clj`, extend approved library reading to consider both selected config-dir `libs.edn` and `libs.local.edn`.
- **TASK-RepoFirstConfig-004.MI2:** Apply the same structural validation grammar to both files: top-level map with `:libs`, symbol coordinates, entry maps, and required non-blank string `:local/root`.
- **TASK-RepoFirstConfig-004.MI3:** Missing files contribute empty libs. Malformed present files fail loudly with enough file context to identify whether `libs.edn` or `libs.local.edn` is bad.
- **TASK-RepoFirstConfig-004.MI4:** Merge shallowly by coordinate with local priority: effective libs are `(merge (:libs libs.edn) (:libs libs.local.edn))`.
- **TASK-RepoFirstConfig-004.MI5:** Preserve relative path, absolute path, `~`, symlink canonicalization, missing-root sync outcome, unreadable-root sync outcome, and runtime-add-failure behavior over the effective config.
- **TASK-RepoFirstConfig-004.MI6:** Add source metadata to normalized approved entries and sync results so callers can identify whether the effective entry came from shared `libs.edn` or local `libs.local.edn`. Exact key names may be chosen during implementation, but the metadata must be present and data-first.
- **TASK-RepoFirstConfig-004.MI7:** Add Clojure tests for missing local file, malformed local file, local-only library, shared-only library, local override winning by coordinate, relative local roots resolving against selected config-dir, and `libs/use!` gates observing overridden effective libs.

## TASK-RepoFirstConfig-004.P3 Done when

- **TASK-RepoFirstConfig-004.DW1:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes for touched Clojure tests.
- **TASK-RepoFirstConfig-004.DW2:** `(libs/approved)` returns the local root from `libs.local.edn` when both files define the same coordinate.
- **TASK-RepoFirstConfig-004.DW3:** `(libs/sync!)` and `(libs/use!)` operate on the effective overlaid config without separate local-specific calls.

## TASK-RepoFirstConfig-004.P4 Out of scope

- **TASK-RepoFirstConfig-004.OS1:** Do not add package install commands, remote source fetching, Maven coordinates, or lockfiles.
- **TASK-RepoFirstConfig-004.OS2:** Do not block or warn on local overrides; override is intentional user control.

## TASK-RepoFirstConfig-004.P5 References

- **TASK-RepoFirstConfig-004.REF1:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md), [REPL API delta](../specs/repl-api.delta.md), [Plan](../repo-first-config.plan.md).
- **TASK-RepoFirstConfig-004.REF2:** Current anchors: `src/skein/weaver/api.clj`, `src/skein/libs/alpha.clj`, `test/skein/libs_test.clj`.
