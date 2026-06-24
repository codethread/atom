# Devflow

Active feature work lives under `devflow/feat/`. Root specs in `devflow/specs/` become canonical only when feature work ships.

Always study [TENETS](./TENETS.md). No code, spec or idea can violate these unless explicitly stated and cited in an agreed RFC first.

## Root specs

Root specs are canonical for shipped behavior:

- [Task Model](./specs/task-model.md) — task records, JSON attributes, edge semantics, and readiness rules.
- [CLI Surface](./specs/cli.md) — scriptable command contract for agents.
- [REPL API](./specs/repl-api.md) — interactive Clojure helper contract.

## Active features

None.

## Archived features

Archived feature folders preserve historical planning context. Current shipped contracts are the root specs above, even if older archive notes describe pre-spec documentation locations.

- `26-06-24__agent-tool-interface` — shipped agent-operable CLI/REPL interface for the todo graph MVP.
- `26-06-24__db-owned-task-ids` — shipped generated task ids and creation-time `--link` edges.
- `26-06-24__batch-task-refs` — shipped stdin EDN batch task creation with batch-local refs.
- `26-06-24__stripped-task-api` — shipped smaller CLI/REPL surface with first-class task lifecycle fields.
