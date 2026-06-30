# Task 2: Route strand init through mill

**Document ID:** `TASK-MillRouterRuntime-002`

## TASK-MillRouterRuntime-002.P1 Scope

Type: AFK

Change `strand init` into a mill-routed repo config bootstrap command. It must not initialize SQLite, start a weaver, or run `git init`.

## TASK-MillRouterRuntime-002.P2 Must implement exactly

- **TASK-MillRouterRuntime-002.MI1:** Make `strand init` require a reachable mill; when mill is unavailable, fail with remediation to run `mill start`.
- **TASK-MillRouterRuntime-002.MI2:** Add a mill operation for repo/world bootstrap that receives cwd plus optional `--config-dir` and resolves the target world.
- **TASK-MillRouterRuntime-002.MI3:** For implicit no-flag init, require cwd to be inside a Git worktree and create/complete `.skein` at the Git toplevel. Outside Git, fail loudly.
- **TASK-MillRouterRuntime-002.MI4:** Preserve existing source resolution for local `config.json`: `--source`, then `SKEIN_SOURCE`, then cwd when cwd is a valid Skein checkout, otherwise fail with remediation.
- **TASK-MillRouterRuntime-002.MI5:** Create only missing config files/directories and never overwrite existing `init.clj`, `libs.edn`, `.gitignore`, or `config.json`.
- **TASK-MillRouterRuntime-002.MI6:** Remove explicit-config-dir `git init` behavior.
- **TASK-MillRouterRuntime-002.MI7:** Ensure `strand init` no longer calls a weaver/database `init` operation.

## TASK-MillRouterRuntime-002.P3 Done when

- **TASK-MillRouterRuntime-002.DW1:** Tests show `strand init` inside a nested Git worktree creates `.skein` at the Git root and not in the nested cwd.
- **TASK-MillRouterRuntime-002.DW2:** Tests show `strand init` outside Git fails and creates no cwd `.skein`.
- **TASK-MillRouterRuntime-002.DW3:** Tests show existing config files are preserved and explicit `--config-dir` bootstrap does not run `git init`.
- **TASK-MillRouterRuntime-002.DW4:** Tests prove no weaver socket/database call is made by `strand init`.
- **TASK-MillRouterRuntime-002.DW5:** `(cd cli && go test ./...)` passes.

## TASK-MillRouterRuntime-002.P4 Out of scope

- **TASK-MillRouterRuntime-002.OS1:** Starting or stopping weavers.
- **TASK-MillRouterRuntime-002.OS2:** Moving Clojure runtime state/data paths.
- **TASK-MillRouterRuntime-002.OS3:** Documentation updates beyond test fixture/help text needed for the command.

## TASK-MillRouterRuntime-002.P5 References

- **TASK-MillRouterRuntime-002.REF1:** [CLI delta](../specs/cli.delta.md)
- **TASK-MillRouterRuntime-002.REF2:** `cli/internal/command`, `cli/internal/config`
- **TASK-MillRouterRuntime-002.REF3:** Current `strand init` implementation in `cli/internal/command/command.go`
