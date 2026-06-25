# Devflow

Active feature work lives under `devflow/feat/`. Root specs in `devflow/specs/` become canonical only when feature work ships.

Always study [TENETS](./TENETS.md) and [PHILOSOPHY](./PHILOSOPHY.md). No code, spec or idea can violate these unless explicitly stated and cited in an agreed RFC first.

## Root specs

Root specs are canonical for shipped behavior:

- [Task Model](./specs/task-model.md) — task records, JSON attributes, edge semantics, and readiness rules.
- [CLI Surface](./specs/cli.md) — scriptable command contract for agents.
- [REPL API](./specs/repl-api.md) — interactive Clojure helper contract.
- [Daemon Runtime](./specs/daemon-runtime.md) — local long-lived daemon lifecycle, metadata, transport, and trusted startup config.

## Active features

- `daemon-query-registry` — in-memory daemon query registry managed through REPL/config workflows and consumed by CLI named queries.
- `go-cli-migration` — planned migration of the thin scripted CLI to a Go client over the daemon's JSON Unix socket; plan deferred until active query registry deltas `SPEC-002-D002` and `SPEC-004-D001` are promoted or otherwise stabilized.

## Archived features

Archived feature folders preserve historical planning context. Current shipped contracts are the root specs above, even if older archive notes describe pre-spec documentation locations.

- `26-06-24__agent-tool-interface` — shipped agent-operable CLI/REPL interface for the todo graph MVP.
- `26-06-24__db-owned-task-ids` — shipped generated task ids and creation-time `--link` edges.
- `26-06-24__batch-task-refs` — shipped stdin EDN batch task creation with batch-local refs.
- `26-06-25__daemon-runtime` — shipped local daemon runtime with nREPL transport, daemon-backed CLI/REPL clients, and trusted startup config.
- `26-06-24__stripped-task-api` — shipped smaller CLI/REPL surface with first-class task lifecycle fields.
