# Task 1: Implement repo-first CLI world selection

**Document ID:** `TASK-RepoFirstConfig-001`

## TASK-RepoFirstConfig-001.P1 Scope

Type: AFK

Implement the Go CLI slice that makes repo-local `.skein` the default selected world, preserves explicit `--config-dir`, and bootstraps repo `.skein` worlds safely.

## TASK-RepoFirstConfig-001.P2 Must implement exactly

- **TASK-RepoFirstConfig-001.MI1:** In `cli/internal/config`, change world resolution so explicit config-dir keeps current behavior, while absent config-dir searches upward from cwd for the nearest `.skein` directory.
- **TASK-RepoFirstConfig-001.MI2:** For absent config-dir and no discovered `.skein`, make non-init commands fail loudly with remediation to run `strand init` or pass `--config-dir`.
- **TASK-RepoFirstConfig-001.MI3:** Change no-flag `strand init` to create or complete `.skein` at the nearest Git root; outside Git, create or complete `.skein` in cwd. Explicit `--config-dir` must skip Git-root discovery.
- **TASK-RepoFirstConfig-001.MI4:** Add `strand init --source <path>` source resolution for local `config.json`; source precedence is `--source`, `SKEIN_SOURCE`, cwd when cwd validates as the Skein source checkout, then fail with remediation.
- **TASK-RepoFirstConfig-001.MI5:** Bootstrap missing `.skein/.gitignore` with defaults ignoring `config.json`, `init.local.clj`, `libs.local.edn`, `state/`, `data/`, `weaver.*`, and SQLite/runtime artifacts. Do not overwrite existing files.
- **TASK-RepoFirstConfig-001.MI6:** Stop creating a nested `.git` inside repo-discovered `.skein` worlds. Preserve existing `strand init --config-dir <dir>` standalone workspace behavior, including `git init` inside that explicit config-dir, because explicit config-dir remains the test/library-workspace escape hatch.
- **TASK-RepoFirstConfig-001.MI7:** Ensure `weaver start` and `weaver repl` pass the resolved selected config-dir to launched Clojure processes even when repo discovery selected it, not only when the user passed `--config-dir`.
- **TASK-RepoFirstConfig-001.MI8:** Add/update Go tests for explicit config-dir precedence, parent `.skein` discovery from a subdirectory, no-world failure, Git-root init, outside-Git init, source precedence, incomplete discovered config remediation, no-overwrite bootstrap, and launched process args.

## TASK-RepoFirstConfig-001.P3 Done when

- **TASK-RepoFirstConfig-001.DW1:** `(cd cli && go test ./...)` passes.
- **TASK-RepoFirstConfig-001.DW2:** A CLI command run from a repo subdirectory selects the repo root `.skein` when no `--config-dir` is supplied.
- **TASK-RepoFirstConfig-001.DW3:** A CLI command outside any `.skein` world fails with a clear no-world remediation instead of using XDG defaults.
- **TASK-RepoFirstConfig-001.DW4:** `strand init --source <skein-source>` can complete an existing committed `.skein` missing local `config.json`.

## TASK-RepoFirstConfig-001.P4 Out of scope

- **TASK-RepoFirstConfig-001.OS1:** Do not implement layered `init.local.clj` or `libs.local.edn`; later tasks own Clojure runtime config behavior.
- **TASK-RepoFirstConfig-001.OS2:** Do not add package installation, remote source fetching, or global config fallback.

## TASK-RepoFirstConfig-001.P5 References

- **TASK-RepoFirstConfig-001.REF1:** [Proposal](../proposal.md), [Plan](../repo-first-config.plan.md), [CLI delta](../specs/cli.delta.md).
- **TASK-RepoFirstConfig-001.REF2:** Current Go anchors: `cli/internal/config/config.go`, `cli/internal/command/command.go`, `cli/integration_test.go`.
