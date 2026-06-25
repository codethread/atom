# Daemon Runtime Delta — Runtime Plugin System

**Document ID:** `RPS-DELTA-001`
**Status:** Draft
**Date:** 2026-06-25
**Blocked by:** `user-daemon-home` spec promotion
**Updates:** [Daemon Runtime](../../../specs/daemon-runtime.md)

## RPS-DELTA-001.P1 Summary

Define Atom's trusted runtime plugin/library model. The daemon loads user code from the selected config-dir world, ships source-visible blessed libraries, records loaded plugin metadata, and treats blessed APIs as recommended stable paths rather than exclusive capability boundaries.

## RPS-DELTA-001.P2 Contract changes

- **RPS-DELTA-001.C1:** A plugin is trusted Clojure runtime code loaded into the daemon process through selected config-dir startup (`init.clj`) or connected REPL workflows.
- **RPS-DELTA-001.C2:** Plugins run with daemon process authority. Sandboxing, untrusted execution, remote authorization, and capability restriction are outside this contract.
- **RPS-DELTA-001.C3:** Atom ships blessed runtime libraries in the source checkout. Users may require these namespaces directly from `init.clj` or connected REPL sessions.
- **RPS-DELTA-001.C4:** Blessed libraries are documented, tested, and used by examples. They are guidance and maintenance promises, not enforcement boundaries.
- **RPS-DELTA-001.C5:** Trusted plugins may depend on lower-level namespaces or raw SQLite schema. Such code is valid but owns the compatibility cost of bypassing blessed APIs.
- **RPS-DELTA-001.C6:** The daemon exposes lightweight plugin metadata registration for loaded libraries: plugin name, version, optional source, optional required Atom version, and provided features.
- **RPS-DELTA-001.C7:** The daemon exposes plugin introspection for trusted REPL/config workflows so users can see what libraries/plugins are loaded in the current daemon lifetime.
- **RPS-DELTA-001.C8:** Plugin metadata is daemon-lifetime runtime state unless a later package-manager feature defines lockfiles or durable plugin manifests.
- **RPS-DELTA-001.C9:** Startup plugin load errors fail daemon startup loudly and publish no ready metadata, following the `user-daemon-home` `init.clj` failure model.

## RPS-DELTA-001.P3 Stability and coupling tiers

- **RPS-DELTA-001.T1:** Blessed public libraries: recommended, documented, tested, and used by Atom examples.
- **RPS-DELTA-001.T2:** Supported low-level libraries: available to trusted plugin code but closer to daemon/database implementation details.
- **RPS-DELTA-001.T3:** Internal implementation namespaces: inspectable and callable by trusted code, but may change freely.
- **RPS-DELTA-001.T4:** Raw SQLite schema: allowed for trusted code, but plugins own compatibility risk if schema or persistence helpers change.

## RPS-DELTA-001.P4 Future package direction

- **RPS-DELTA-001.F1:** A future package feature may adopt a `straight.el`-inspired model where config plus lockfile are the sources of truth.
- **RPS-DELTA-001.F2:** Future plugin recipes should be simple data maps describing source URL, branch/ref, files, deps, and optional build/load behavior.
- **RPS-DELTA-001.F3:** Future package resolution should prefer user overrides and direct source control access over centralized hidden state.

## RPS-DELTA-001.P5 Non-goals retained

- **RPS-DELTA-001.N1:** Do not add a plugin marketplace, package registry, or dependency solver in this feature.
- **RPS-DELTA-001.N2:** Do not persist plugin functions or runtime behavior in SQLite.
- **RPS-DELTA-001.N3:** Do not expose arbitrary plugin loading through the low-privilege JSON CLI surface.
