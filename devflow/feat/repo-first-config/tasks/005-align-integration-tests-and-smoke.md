# Task 5: Align integration tests and smoke

**Document ID:** `TASK-RepoFirstConfig-005`

## TASK-RepoFirstConfig-005.P1 Scope

Type: AFK

Align end-to-end tests, smoke workflows, and repo-local scenarios after CLI selection, layered init, and library overrides are implemented.

## TASK-RepoFirstConfig-005.P2 Must implement exactly

- **TASK-RepoFirstConfig-005.MI1:** Update integration tests and smoke helpers that relied on no-flag XDG default worlds to use explicit `--config-dir` disposable worlds unless they are explicitly testing repo discovery.
- **TASK-RepoFirstConfig-005.MI2:** Add an end-to-end repo-local scenario where `strand init --source <skein-source>` creates `.skein`, `strand weaver start` launched from a subdirectory uses that `.skein`, and strand commands from the subdirectory operate against that world without `--config-dir`.
- **TASK-RepoFirstConfig-005.MI3:** Add an end-to-end local overlay scenario that registers behavior from `init.local.clj` and confirms it is available through CLI/REPL-facing behavior after startup.
- **TASK-RepoFirstConfig-005.MI4:** Add or adjust smoke coverage only where it adds confidence not already covered by focused Go/Clojure tests; keep smoke disposable and ensure it does not mutate the user's default world.
- **TASK-RepoFirstConfig-005.MI5:** Ensure validation cleanup leaves no `.sqlite`, `weaver.json`, `weaver.edn`, `weaver.sock`, or repo-local runtime artifacts outside temp worlds.

## TASK-RepoFirstConfig-005.P3 Done when

- **TASK-RepoFirstConfig-005.DW1:** `(cd cli && go test ./...)` passes.
- **TASK-RepoFirstConfig-005.DW2:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes.
- **TASK-RepoFirstConfig-005.DW3:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke` passes.
- **TASK-RepoFirstConfig-005.DW4:** `git status --short` after validation shows no generated runtime artifacts.

## TASK-RepoFirstConfig-005.P4 Out of scope

- **TASK-RepoFirstConfig-005.OS1:** Do not add broad manual QA scripts or package-manager behavior.
- **TASK-RepoFirstConfig-005.OS2:** Do not promote specs; task 5 owns durable documentation/spec promotion.

## TASK-RepoFirstConfig-005.P5 References

- **TASK-RepoFirstConfig-005.REF1:** [Plan validation strategy](../repo-first-config.plan.md#PLAN-RepoFirstConfig-001.P6), [CLI delta](../specs/cli.delta.md).
- **TASK-RepoFirstConfig-005.REF2:** Current anchors: `cli/integration_test.go`, `dev/skein/smoke.clj`, project validation guidance in `AGENTS.md`.
