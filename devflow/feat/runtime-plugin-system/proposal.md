# Runtime Plugin System Proposal

**Document ID:** `RPS-PROP-001`
**Status:** Draft
**Date:** 2026-06-25
**Blocked by:** `devflow/feat/user-daemon-home` shipping config-dir daemon worlds, default `init.clj`, and connected REPL workflows
**Related PRD:** [Runtime Transformations PRD](../../prd/runtime-transformations.md)
**Research:** [`straight.el`](https://github.com/radian-software/straight.el), vendored for study at `~/dev/vendor/straight.el`
**Related RFCs:** None
**Relevant root specs:** [Daemon Runtime](../../specs/daemon-runtime.md), [REPL API](../../specs/repl-api.md), [CLI Surface](../../specs/cli.md)

## RPS-PROP-001.P1 Problem

Atom wants runtime transformations, but adding every useful query, graph, view, or workflow primitive directly to daemon core risks turning core into the extension system. That conflicts with the Emacs-shaped philosophy: trusted users should be able to inspect the shipped source, load libraries, replace conventions, couple to lower-level APIs when worth it, and share better approaches.

The missing abstraction is not another bespoke view primitive. It is a small trusted runtime plugin/library model: Atom ships blessed libraries and a bootstrap/prelude path, while users can load their own Clojure code from `init.clj`, use or ignore the blessed path, and knowingly accept compatibility costs when they depend on lower-level internals or raw schema.

## RPS-PROP-001.P2 Goals

- **RPS-PROP-001.G1:** Define Atom plugins as trusted Clojure runtime libraries loaded by daemon startup config or connected REPL workflows.
- **RPS-PROP-001.G2:** Make the shipped source tree part of the user-facing extension story: users can require Atom libraries, inspect source, and write compatible or alternative libraries.
- **RPS-PROP-001.G3:** Establish blessed library conventions with maintenance promises, not capability boundaries.
- **RPS-PROP-001.G4:** Define stability/coupling tiers so users know the price of bypassing blessed libraries.
- **RPS-PROP-001.G5:** Add an ergonomic `atom.bootstrap` or `atom.prelude` path that loads the recommended default library set from `init.clj`.
- **RPS-PROP-001.G6:** Preserve autonomy: plugins may use blessed APIs, lower-level namespaces, or raw SQLite/schema access if they accept the maintenance cost.
- **RPS-PROP-001.G7:** Keep the public CLI thin. The CLI may manage/inspect runtime/plugin state later, but it does not become an extension authoring surface.

## RPS-PROP-001.P3 Non-goals

- **RPS-PROP-001.NG1:** Do not build a marketplace, package registry, or full dependency solver.
- **RPS-PROP-001.NG2:** Do not sandbox plugins or support untrusted plugin execution.
- **RPS-PROP-001.NG3:** Do not prevent raw DB/schema coupling by trusted user code.
- **RPS-PROP-001.NG4:** Do not add public CLI view invocation as part of this feature.
- **RPS-PROP-001.NG5:** Do not implement full `straight.el` parity: no recipe repositories, autoload generation, native compilation, profiles, or package pruning in the first slice.

## RPS-PROP-001.P4 Proposed scope

- **RPS-PROP-001.S1:** Specify the plugin/library model: trusted Clojure code, source-visible, loaded through selected config-dir `init.clj` or connected REPL.
- **RPS-PROP-001.S2:** Define stability/coupling tiers: blessed libraries, supported low-level libraries, internal implementation, and raw schema access.
- **RPS-PROP-001.S3:** Add initial shipped namespaces for bootstrap/prelude conventions, even if their first implementation is small.
- **RPS-PROP-001.S4:** Define minimal plugin metadata conventions for loaded libraries, such as name, version, Atom version expectations, and provided features.
- **RPS-PROP-001.S5:** Define introspection helpers for loaded plugin/library metadata.
- **RPS-PROP-001.S6:** Document a recommended `init.clj` default such as requiring `atom.bootstrap` and calling `(use-defaults!)`.
- **RPS-PROP-001.S7:** Capture a future-compatible package/recipe direction inspired by `straight.el`: config plus lockfile as sources of truth, user overrides, and git-backed plugin repos, without implementing the full package manager in this slice.

## RPS-PROP-001.P5 Open questions

- **RPS-PROP-001.Q1:** Should the blessed namespace prefix be `atom.*` even while implementation namespaces still live under `todo.*`, or should the codebase rename first? Prefer `atom.*` for new public libraries.
- **RPS-PROP-001.Q2:** Should `atom.bootstrap/use-defaults!` only require libraries, or also register default queries/views once those libraries exist? Prefer requiring libraries now and registering behavior only when a later feature defines it.
- **RPS-PROP-001.Q3:** How much package recipe/lockfile machinery belongs in this first feature versus a later `runtime-plugin-packages` feature? Prefer spec-only for recipes/locks now, implementation later.
