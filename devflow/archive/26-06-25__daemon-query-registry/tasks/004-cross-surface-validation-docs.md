# Cross Surface Validation and Docs

**Document ID:** `TASK-004`

## TASK-004.P1 Scope

Type: AFK

Prove and document that REPL/config-loaded queries are shared through daemon memory and consumable from the CLI during one daemon lifetime.

## TASK-004.P2 References

- **TASK-004.R1:** [Feature plan](../daemon-query-registry.plan.md)
- **TASK-004.R2:** [CLI delta](../specs/cli.delta.md), [REPL delta](../specs/repl-api.delta.md), [Daemon delta](../specs/daemon-runtime.delta.md)
- **TASK-004.R3:** `README.md`, `AGENTS.md`, `dev/todo/smoke.clj`, root specs under `devflow/specs/`

## TASK-004.P3 Implementation notes

- **TASK-004.I1:** Add or update integration/smoke coverage showing REPL/client registers or loads a query and CLI uses it by name.
- **TASK-004.I2:** Add or update coverage showing CLI `--query-file` is no longer accepted.
- **TASK-004.I3:** Update user-facing docs and root specs for the shipped command/helper behavior.
- **TASK-004.I4:** Include daemon-lifetime limitation clearly: registry contents are in memory and disappear when the daemon stops.
- **TASK-004.I5:** Reference the daemon-core philosophy in docs where explaining why runtime query loading is REPL/config-owned rather than CLI-owned.

## TASK-004.P4 Done when

- **TASK-004.D1:** REPL/config-to-CLI query reuse is covered by tests or smoke validation.
- **TASK-004.D2:** Docs/specs describe REPL/config loading, CLI use, and lifetime of named queries.
- **TASK-004.D3:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes.
- **TASK-004.D4:** No generated SQLite/runtime artifacts remain in `git status --short`.
