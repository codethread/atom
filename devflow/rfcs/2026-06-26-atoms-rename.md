# Rename to Atoms: chemistry-metaphor vocabulary and namespace move

**Document ID:** `RFC-006`
**Status:** Accepted
**Date:** 2026-06-26
**Related:** [PHILOSOPHY](../PHILOSOPHY.md), [TENETS](../TENETS.md), [SPEC-001 Task Model](../specs/task-model.md), [SPEC-002 CLI](../specs/cli.md), [SPEC-003 REPL API](../specs/repl-api.md), [SPEC-004 Daemon Runtime](../specs/daemon-runtime.md), [PRD-001 Runtime Transformations](../prd/runtime-transformations.md)

## RFC-006.P1 Problem

The project shipped as a "todo" graph but has grown (per [PRD-001](../prd/runtime-transformations.md)) into a general attributed-DAG core where the only durable facts are records, their open JSON attributes, and typed edges; everything else is runtime transformation. The naming no longer matches that reality, and it is inconsistent:

- the public Go CLI binary is `todo`;
- internal Clojure namespaces are `todo.*` (`todo.daemon.api`, `todo.repl`, `src/todo/`);
- blessed runtime libraries are `atom.*.alpha` (`atom.libs.alpha`, `atom.graph.alpha`, `atom.views.alpha`);
- the stored unit is a `task`; storage is `tasks` / `task_edges` in `tasks.sqlite`;
- runtime worlds live under `atom` config/state/data dirs.

"todo" misdescribes a neutral primitive that users should stamp meaning onto (`kind=task|note|idea|page`), and the split between `todo.*` and `atom.*` is incoherent. We should settle one identity and vocabulary now, before more libraries and specs harden these names.

## RFC-006.P2 Goals

- **RFC-006.G1:** One coherent product, CLI, and namespace identity.
- **RFC-006.G2:** A maximally neutral stored primitive so userland supplies domain meaning via attributes.
- **RFC-006.G3:** Vocabulary that reinforces the pure-core / runtime-transformation split: durable facts vs derived structure.
- **RFC-006.G4:** A verb-friendly CLI surface for the common mutations.
- **RFC-006.G5:** Drop legacy names outright per [TEN-000](../TENETS.md) without compatibility shims or data migration.

## RFC-006.P3 Non-goals

- **RFC-006.NG1:** No change to storage engine or contract semantics beyond renames; SQLite persistence, the acyclic invariant ([TEN-005](../TENETS.md)), open attributes, and lifecycle states keep their current meaning.
- **RFC-006.NG2:** No new behavior; this is a rename, not a query/graph/view feature.
- **RFC-006.NG3:** Does not finalize the full flag syntax of the new `bond` CLI verb; that is spec/plan work.
- **RFC-006.NG4:** Does not touch remote access, auth, sandboxing, or multi-user concerns.

## RFC-006.P4 Options

| ID | Summary | Pros | Cons |
| ------------ | ------- | ---- | ---- |
| RFC-006.O1 | Keep status-quo mixed naming (`todo.*` code, `atom.*` libs, `task` unit) | No work | Incoherent; "todo" is inaccurate; bakes confusion into more specs/libs |
| RFC-006.O2 | Unit `atom`; two binaries `atom` (client) + `atoms` (daemon) | Cute singular/plural symmetry | One-letter-apart binaries collide on tab-completion and typos |
| RFC-006.O3 | Organic unit family (`strand`, `knot`, …) | Evocative; connectable | `strand` verb is negative ("stranded"); `knot` is a homophone of "not", bad for a text-first agent tool |
| RFC-006.O4 | **`atoms` as the single identity (project + CLI + namespace root); `reactor` as the daemon; data vocabulary `atom` / `bond` / `molecule`** | Coherent; neutral primitive; free chemistry vocab; daemon name removes the O2 collision | Broad rename; minor Clojure `atom`-symbol hygiene cost |

## RFC-006.P5 Recommendation

- **RFC-006.REC1:** Adopt **RFC-006.O4**. Use `atoms` as the one identity for the project, the CLI binary, the Clojure namespace root, and the runtime world dirs. Name the daemon **`reactor`** — the long-lived engine where atoms bond and transform, which matches PRD-001's "almost everything is runtime transformation over durable facts" thesis and removes the two-binary collision because `atom` is then only ever a noun, never a command. Keep the metaphor at the semantic surface; the schema stays literal so agents still grep durable facts ([TEN-001](../TENETS.md), [TEN-003](../TENETS.md)).

- **RFC-006.REC2:** Canonical vocabulary mapping:

  | Concept | New name | Was |
  | ------- | -------- | --- |
  | Product / CLI binary / namespace root / world dirs | **atoms** | `todo` / `atom` |
  | Long-lived daemon / runtime engine | **reactor** | daemon |
  | The durable stored unit (a noun) | **atom** | task |
  | A typed, directed edge | **bond** | edge |
  | A connected subgraph (derived, never stored) | **molecule** | subgraph / "DAG" |
  | Blessed runtime libraries | **`atoms.libs.alpha`** / **`atoms.graph.alpha`** / **`atoms.views.alpha`** | `atom.*.alpha` |

  `molecule` is a transformation concept owned by `atoms.graph.alpha` / userland, never a stored table (PRD-001.G1). Bond *types* (`depends-on`, `related-to`, `parent-of`, `supersedes`) are unchanged; "directed bond" simply accepts that these edges are directional even though chemical bonds are not.

## RFC-006.P6 Consequences

- **RFC-006.C1:** CLI ([SPEC-002](../specs/cli.md)): Go binary `todo` → `atoms`; `daemon start|stop|status` subcommands → `reactor start|stop|status`; add a first-class `atoms bond <from> <to> <type>` verb so edge creation no longer rides only on `update --edge`.
- **RFC-006.C2:** Internal Clojure namespaces ([SPEC-003](../specs/repl-api.md), [SPEC-004](../specs/daemon-runtime.md)): `todo.*` → `atoms.*` (`todo.daemon.api` → `atoms.reactor.api`, `todo.repl` → `atoms.repl`, `src/todo/` → `src/atoms/`).
- **RFC-006.C3:** Blessed runtime libraries (SPEC-004.P9/P10, PRD-001 examples): `atom.libs.alpha` / `atom.graph.alpha` / `atom.views.alpha` → the `atoms.*` equivalents.
- **RFC-006.C4:** Storage ([SPEC-001](../specs/task-model.md)): tables `tasks` → `atoms`, `task_edges` → `bonds`; default db `tasks.sqlite` → `atoms.sqlite`. Column meanings (`status`, `attributes`, `final_at`, timestamps) are unchanged.
- **RFC-006.C5:** Runtime worlds (SPEC-004.P2/P3): config `~/.config/atom` → `~/.config/atoms`, state `~/.local/state/atom` → `~/.local/state/atoms`, data `~/.local/share/atom` → `~/.local/share/atoms`. Whether socket/metadata files rename `daemon.sock`/`daemon.json`/`daemon.edn` → `reactor.*` is a small open item for the spec stage.
- **RFC-006.C6:** Specs: rename `task-model.md` → `atom-model.md`; refresh vocabulary across `cli.md`, `daemon-runtime.md` (→ reactor framing), `repl-api.md`, and PRD-001. Promote on feature finish, not in this RFC.
- **RFC-006.C7:** Code hygiene: never bind a bare Clojure var named `atom` (it would shadow `clojure.core/atom`); always qualify (`db/add-atom!`, `graph/bond!`). Namespaces under `atoms.*` do not collide with the core `atom` fn.
- **RFC-006.C8:** Per [TEN-000](../TENETS.md), drop legacy names with no compatibility aliases and no data migration; existing local worlds/dbs are disposable.

## RFC-006.P7 Outcome

- **RFC-006.OUT1:** Accepted 2026-06-26 by the project owner. Direction: rename the project, CLI, and namespace root to **atoms**; name the daemon **reactor**; adopt the **atom / bond / molecule** data vocabulary; move blessed libraries to `atoms.*`. Follow-up: open a feature folder (e.g. `atoms-rename`) carrying spec deltas for SPEC-001 (→ atom-model), SPEC-002, SPEC-003, SPEC-004 (→ reactor runtime), and PRD-001, plus a plan that sequences the binary, namespace, library, schema, and world-dir renames. Open implementation items deferred to that feature: daemon binary vs `atoms reactor` subcommand (leaning subcommand), socket/metadata file naming, and the exact `bond` verb flag contract.
