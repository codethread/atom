# Runtime Plugin System Plan

**Document ID:** `RPS-PLAN-001`
**Status:** Draft
**Last Updated:** 2026-06-25
**Blocked by:** `devflow/feat/user-daemon-home` shipping and promoting config-dir daemon worlds, default `init.clj`, `connect!`, and connected `todo daemon repl --stdin`
**Proposal:** [proposal.md](./proposal.md)
**Spec deltas:** [daemon-runtime.delta.md](./specs/daemon-runtime.delta.md), [repl-api.delta.md](./specs/repl-api.delta.md), [cli.delta.md](./specs/cli.delta.md)
**Related PRD:** [Runtime Transformations PRD](../../prd/runtime-transformations.md)
**Research:** `straight.el` docs/source; vendored at `~/dev/vendor/straight.el`
**Related RFCs:** None

## RPS-PLAN-001.P1 Goal and scope

Establish Atom's Emacs-like trusted runtime plugin/library model before building runtime transformation libraries. This feature defines blessed source-visible libraries, plugin metadata/introspection, bootstrap/prelude ergonomics, and stability/coupling tiers. It does not implement a package manager, marketplace, dependency solver, or public CLI plugin loading surface.

## RPS-PLAN-001.P2 Approach

- **RPS-PLAN-001.A1:** Treat the shipped source checkout as part of the extension system. Users can inspect and require Atom namespaces directly.
- **RPS-PLAN-001.A2:** Make `init.clj` the daemon-world entrypoint for trusted runtime customization after `user-daemon-home` lands.
- **RPS-PLAN-001.A3:** Provide blessed libraries as conventions with maintenance promises, not capability boundaries. Users may use lower-level namespaces or raw SQLite when they accept the cost.
- **RPS-PLAN-001.A4:** Add small plugin metadata and introspection primitives so loaded runtime libraries are visible during a daemon lifetime.
- **RPS-PLAN-001.A5:** Add `atom.bootstrap` for side-effectful default setup and `atom.prelude` for optional broad REPL convenience imports.
- **RPS-PLAN-001.A6:** Capture `straight.el`'s useful lessons as future direction: config plus lockfile as sources of truth, git recipes, user overrides, reproducibility, and no hidden package state.
- **RPS-PLAN-001.A7:** Defer actual git package sync/freeze/thaw commands. First make local/source-shipped plugin semantics clear and useful.

## RPS-PLAN-001.P3 Affected areas

| ID | Area | Impact |
| --- | --- | --- |
| RPS-PLAN-001.AA1 | `src/atom/*` or equivalent public namespaces | Add blessed bootstrap/prelude/plugin namespaces. |
| RPS-PLAN-001.AA2 | `src/todo/daemon/api.clj` | Add or expose daemon-lifetime plugin metadata registry operations. |
| RPS-PLAN-001.AA3 | `src/todo/repl.clj` | Expose plugin metadata/introspection helpers in connected REPL workflows. |
| RPS-PLAN-001.AA4 | `devflow/specs/*` | Stage daemon/repl/cli contract changes around trusted plugins. |
| RPS-PLAN-001.AA5 | `README.md`, `AGENTS.md`, smoke docs | Show `init.clj` using `atom.bootstrap/use-defaults!` and explain autonomy/coupling tiers. |
| RPS-PLAN-001.AA6 | Tests/smoke | Verify startup plugin load, metadata introspection, and connected REPL behavior after `user-daemon-home`. |

## RPS-PLAN-001.P4 Contract and migration impact

- **RPS-PLAN-001.CM1:** This feature depends on `user-daemon-home`; do not target database-path `open!` or public `daemon start --config` workflows.
- **RPS-PLAN-001.CM2:** The first plugin system is source/local runtime loading, not remote package installation.
- **RPS-PLAN-001.CM3:** Plugin metadata is daemon-lifetime state. Future package lockfiles are separate.
- **RPS-PLAN-001.CM4:** New `atom.*` blessed namespace names may coexist with existing `todo.*` implementation namespaces. If this creates naming friction, record it before implementation.

## RPS-PLAN-001.P5 Implementation phases

### RPS-PLAN-001.PH1 Plugin contract and stability tiers

Finalize spec wording for trusted plugins, blessed libraries, lower-level/internal/raw schema tiers, and user-owned compatibility costs. Ensure docs say the blessed path is recommended but not mandatory.

### RPS-PLAN-001.PH2 Plugin metadata registry

Add daemon-lifetime plugin metadata registration and introspection. Keep the data shape small: name, version, optional source, optional Atom version expectation, and provided features.

### RPS-PLAN-001.PH3 Bootstrap and prelude namespaces

Add `atom.bootstrap/use-defaults!` and `atom.prelude`. In this first feature, defaults may be intentionally small: register Atom's base libraries/features and prepare the namespace shape for later query/graph/view libraries.

### RPS-PLAN-001.PH4 Connected REPL and init examples

Update docs and smoke examples to show a config-dir `init.clj` requiring `atom.bootstrap` and using plugin introspection from `todo daemon repl --stdin`.

### RPS-PLAN-001.PH5 Future package direction notes

Record the `straight.el`-inspired package direction without implementing it: recipes, user overrides, config plus lockfile as sources of truth, and git-backed plugin repos.

## RPS-PLAN-001.P6 Validation strategy

- **RPS-PLAN-001.V1:** Clojure tests cover plugin metadata registration, replacement or duplicate behavior, validation, and introspection.
- **RPS-PLAN-001.V2:** Clojure tests cover `atom.bootstrap/use-defaults!` and `atom.prelude` loadability from the configured source checkout.
- **RPS-PLAN-001.V3:** Integration/smoke covers selected config-dir `init.clj` loading bootstrap defaults and querying loaded plugin metadata through connected REPL/stdin.
- **RPS-PLAN-001.V4:** Full validation remains `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test`, `(cd cli && go test ./...)`, and `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke`.

## RPS-PLAN-001.P7 Risks and open questions

- **RPS-PLAN-001.R1:** Introducing `atom.*` namespaces while code still uses `todo.*` may create public/internal naming tension. Mitigation: treat `atom.*` as the blessed user-facing library layer and keep `todo.*` as implementation until a rename is warranted.
- **RPS-PLAN-001.R2:** Users may over-couple to internals and later experience breakage. Mitigation: document tiers and make the cost explicit rather than restricting autonomy.
- **RPS-PLAN-001.R3:** A package manager could distract from runtime transformations. Mitigation: keep package sync/freeze/thaw as future direction only.
- **RPS-PLAN-001.Q1:** Should duplicate plugin metadata registration replace or fail? Prefer replace during daemon startup/reload workflows unless review finds that hiding duplicates is worse.
- **RPS-PLAN-001.Q2:** Should `atom.bootstrap/use-defaults!` perform side effects beyond metadata registration before blessed query/graph/view libraries exist? Prefer minimal side effects.

## RPS-PLAN-001.P8 Task context

- **RPS-PLAN-001.TC1:** This feature is blocked until `user-daemon-home` ships or the human explicitly records a decision to proceed earlier.
- **RPS-PLAN-001.TC2:** Do not implement git package fetching, lockfiles, dependency solving, package registries, or CLI plugin package commands in this feature.
- **RPS-PLAN-001.TC3:** Do not restrict trusted code from lower-level namespace or raw DB access.
- **RPS-PLAN-001.TC4:** Keep all examples Emacs-like: source-visible libraries, `init.clj`, connected REPL, user autonomy.
- **RPS-PLAN-001.TC5:** The next likely feature after this is blessed runtime libraries for query/graph/view behavior that satisfy the Runtime Transformations PRD MVP.

## RPS-PLAN-001.P9 Developer Notes

### RPS-PLAN-001.DN1 Pivot from runtime view primitives — 2026-06-25

Dropped the earlier `runtime-view-primitives` feature draft. User clarified that Atom should first define a plugin/library model: blessed APIs are recommended and maintained, but users remain free to write or adopt better APIs and couple to the DB/schema when they accept the price. This better matches the Emacs philosophy and makes Atom's own future query/graph/view libraries plugin-style libraries rather than privileged daemon magic.

### RPS-PLAN-001.DN2 straight.el research — 2026-06-25

Studied `straight.el` and vendored it to `~/dev/vendor/straight.el` for source inspection. Useful lessons: config plus lockfile as sources of truth, recipe maps, user overrides, git-backed source repos, reproducible freeze/thaw, and no hidden package database. For Atom's first slice, keep only the philosophy and future direction; defer package manager implementation.
