# Mill-Owned Source and Repository-Scoped Weaver Proposal

- **Document ID:** `MOS-PROP-001`
- **Last Updated:** 2026-06-30
- **Related RFCs:** None
- **Related root specs:** [CLI Surface](../../specs/cli.md), [Weaver Runtime](../../specs/daemon-runtime.md)

## MOS-PROP-001.P1 Problem

Skein currently persists the Skein source checkout path in each selected config workspace's `config.json`. That makes source checkout knowledge part of user/repo config even though it is only needed by the local `mill` process to choose the Clojure working directory when launching helper processes and weavers.

The larger worktree problem is selected-world identity. Skein's main strength is a repository-scoped weaver: users can create strands for a whole repository, spread implementation across many Git worktrees, and fan completed work back into one coherent task graph. Defaulting selected config to the current worktree root contradicts that model because each linked worktree can accidentally create or select a different `.skein` workspace and therefore a different weaver, init stack, socket, and strand store.

By default, `strand` should resolve to the Git repository's canonical config route, not the current worktree root. Worktree-local weavers remain useful for Skein development and isolated testing, but they should be opt-in through explicit `--config-dir`, not the default path for contributors working on ordinary source repositories.

## MOS-PROP-001.P2 Goals

- **MOS-PROP-001.G1:** Make source checkout resolution mill-owned runtime knowledge rather than selected config workspace data.
- **MOS-PROP-001.G2:** Make the default weaver repository-scoped across Git worktrees, so all worktrees for one repository talk to the same selected config workspace and strand store.
- **MOS-PROP-001.G3:** Keep `strand` a thin JSON control surface: it selects a repository/world and asks mill to route/start processes; it does not own source persistence.
- **MOS-PROP-001.G4:** Keep selected config workspaces focused on trusted repo/user config and library workspace files, not machine-local launch metadata.
- **MOS-PROP-001.G5:** Preserve explicit `--config-dir` as the opt-in escape hatch for isolated worktree-local Skein development and tests.
- **MOS-PROP-001.G6:** Fail loudly when mill cannot resolve an appropriate Skein source checkout for a selected world.

## MOS-PROP-001.P3 Non-goals

- **MOS-PROP-001.NG1:** No global/default user world fallback or implicit mutation of user-owned config/data/state outside the selected world.
- **MOS-PROP-001.NG2:** No package manager, source installer, Git clone automation, or dependency solver.
- **MOS-PROP-001.NG3:** No default per-worktree weavers. Worktree-local weaver worlds are only for explicit `--config-dir` isolation.
- **MOS-PROP-001.NG4:** No support for stale `source` values in `config.json` as a compatibility path unless an accepted implementation plan explicitly keeps a temporary migration note.
- **MOS-PROP-001.NG5:** No change to where the weaver loads startup files: `init.clj` and `init.local.clj` remain selected-config-dir files, but the default selected config-dir is repository-canonical rather than current-worktree-local.

## MOS-PROP-001.P4 Proposed scope

- **MOS-PROP-001.S1:** Remove source checkout identity from the durable selected config workspace contract; `config.json` should no longer be the authority for the Skein source checkout.
- **MOS-PROP-001.S2:** Define mill as the owner of source checkout resolution for weaver startup and connected helper REPL startup.
- **MOS-PROP-001.S3:** Define default selected-world resolution as Git-repository canonical, not Git-worktree local. All linked worktrees for the same repository must resolve to the same default config workspace and therefore the same weaver/runtime/data identity.
- **MOS-PROP-001.S4:** Define mill source resolution as request-local and fail-loud: prefer `SKEIN_SOURCE`, then the installed build source, then the canonical repository route when it is a Skein checkout containing `deps.edn`. A resolved source must be absolute, existing, and contain `deps.edn`; no public CLI command persists or supplies source as selected-world config.
- **MOS-PROP-001.S5:** Preserve explicit `--config-dir` as the highest-precedence world selector. Explicit config dirs intentionally create independent selected worlds and are the supported path for worktree-local Skein development/testing before merging changes back.
- **MOS-PROP-001.S6:** Preserve explicit `--config-dir` worlds without inferring source from the config-dir path. When an explicit config-dir is not associated with a Skein checkout cwd, mill startup must rely on `SKEIN_SOURCE` or installed build source and fail loudly if none resolves.
- **MOS-PROP-001.S7:** Ensure a single mill can route requests from multiple worktrees for one repository to the same default weaver, while still routing explicit config-dir requests to their isolated worlds.
- **MOS-PROP-001.S8:** Keep runtime state and data under mill-owned XDG paths keyed by selected config identity; moving source ownership and repository-canonical default selection must not move SQLite data or weaver metadata back into `.skein`.
- **MOS-PROP-001.S9:** Update the CLI and Weaver Runtime specs to describe mill-owned source resolution, repository-canonical default world selection, the reduced config workspace contract, and fail-loud source resolution errors.
- **MOS-PROP-001.S10:** Update bootstrap behavior so `strand init` creates/completes trusted config workspace files at the repository-canonical default location without requiring a persisted source path.

## MOS-PROP-001.P5 Open questions

- **MOS-PROP-001.Q1:** The implementation plan should define the exact Git canonical route used to map linked worktrees onto one default selected config workspace, including the expected behavior for unusual Git layouts such as bare repositories or missing main worktree paths.
- **MOS-PROP-001.Q2:** The implementation plan should choose the exact mill-side metadata/storage shape for any runtime source-resolution cache, if one is needed.
