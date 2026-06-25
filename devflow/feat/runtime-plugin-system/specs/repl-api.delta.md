# REPL API Delta — Runtime Plugin System

**Document ID:** `RPS-DELTA-002`
**Status:** Draft
**Date:** 2026-06-25
**Blocked by:** `user-daemon-home` spec promotion
**Updates:** [REPL API](../../../specs/repl-api.md)

## RPS-DELTA-002.P1 Summary

Expose plugin/library ergonomics through the connected REPL: users can require blessed libraries, bootstrap default helpers, register plugin metadata, and inspect loaded runtime libraries.

## RPS-DELTA-002.P2 Contract changes

- **RPS-DELTA-002.C1:** Add a blessed bootstrap namespace, tentatively `atom.bootstrap`, intended for use from selected config-dir `init.clj`.
- **RPS-DELTA-002.C2:** `atom.bootstrap/use-defaults!` loads the recommended Atom runtime library set for users who want the blessed path with minimal setup.
- **RPS-DELTA-002.C3:** Add a prelude namespace, tentatively `atom.prelude`, for common interactive helpers when a user explicitly wants broad REPL convenience imports.
- **RPS-DELTA-002.C4:** Add plugin metadata helpers, tentatively `register-plugin!` and `plugins`, for trusted code to record and inspect loaded runtime libraries.
- **RPS-DELTA-002.C5:** `todo daemon repl` and `todo daemon repl --stdin` from `user-daemon-home` are the blessed ways for users/agents to evaluate plugin code against a running daemon world.
- **RPS-DELTA-002.C6:** REPL docs must distinguish blessed library use from lower-level/internal use without forbidding the latter.

## RPS-DELTA-002.P3 Example init

A minimal recommended config-dir `init.clj` may look like:

```clojure
(require '[atom.bootstrap :as atom])

(atom/use-defaults!)
```

Advanced users may instead require specific Atom libraries or their own plugin namespaces directly.

## RPS-DELTA-002.P4 Non-goals retained

- **RPS-DELTA-002.N1:** Do not require users to use the prelude or defaults.
- **RPS-DELTA-002.N2:** Do not hide lower-level namespaces from trusted REPL users.
- **RPS-DELTA-002.N3:** Do not add a CLI plugin authoring surface.
