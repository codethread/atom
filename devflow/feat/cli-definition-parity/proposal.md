# CLI Definition Parity Proposal

**Document ID:** `CDP-PROP-001`
**Last Updated:** 2026-07-01
**Related RFCs:** None (direction from in-session review; see CDP-PROP-001.Q4)
**Related root specs:** [CLI Surface](../../specs/cli.md), [Weaver Runtime](../../specs/daemon-runtime.md), [REPL API](../../specs/repl-api.md)

## CDP-PROP-001.P1 Problem

Skein exposes several named weaver-side definitions that the thin CLI invokes by name, but their CLI surfaces are inconsistent — the gap the owner named as "align patterns and weaves in the CLI":

- **Patterns** (write templates) have introspection: `pattern list` and `pattern explain <name>`, and are applied via `weave --pattern <name>` (SPEC-002.C13b, SPEC-002.C13a).
- **Queries** (read templates) have none. To discover which queries exist or what params they take, a caller must drop into `strand weaver repl`, because query registry listing/inspection is excluded from the CLI (SPEC-002.C13) and from the JSON socket allowlist (SPEC-004.C27). They are only applied via `list --query <name>` / `ready --query <name>`.

For an agent-first tool (TEN-001), forcing a REPL detour just to discover read templates is a papercut, and the asymmetry makes the "invoke a named weaver definition" model harder to learn. The exclusion was a deliberate anti-mutation stance, but it also blocks read-only introspection that patterns already allow.

Separately, the relationship between `weave` (named, spec-checked, CLI-safe, create-only) and the raw `batch` primitive (REPL-only; create/update/burn/edges) is underdocumented, so an agent reaching for a multi-strand write is surprised the general primitive is REPL-only rather than a CLI verb.

## CDP-PROP-001.P2 Goals

- **CDP-PROP-001.G1:** Give queries read-only CLI introspection at parity with patterns: `query list` and `query explain <name>`.
- **CDP-PROP-001.G2:** Present one coherent, learnable model for named weaver definitions in the CLI — uniform `<type> list` / `<type> explain` introspection — while keeping application shapes that reflect read vs write (`list`/`ready --query`, `weave --pattern`).
- **CDP-PROP-001.G3:** Document the `weave` (CLI-safe front door) ↔ `batch` (REPL-only loading dock) relationship explicitly: one transactional engine, two doors, distinct trust tiers.
- **CDP-PROP-001.G4:** Keep the CLI thin (TEN-006): introspection is read-only; defining/registering queries and patterns stays a trusted config/REPL workflow.

## CDP-PROP-001.P3 Non-goals

- **CDP-PROP-001.NG1:** No query or pattern registry mutation via CLI — no `query add`, no `--query-file`, no pattern registration. SPEC-002.C13/C13b stand for mutation.
- **CDP-PROP-001.NG2:** No CLI-exposed raw batch or arbitrary JSON graph patch; SPEC-002.C22 stands.
- **CDP-PROP-001.NG3:** No rename of the `query`, `pattern`, or `weave` verbs/nouns. They are accurate; `SR-PLAN-001.A3` keeps generic verbs and `pattern`/`weave` already read well.
- **CDP-PROP-001.NG4:** No new query DSL authoring surface on the CLI; rich definitions stay EDN in trusted config/REPL.
- **CDP-PROP-001.NG5:** No change to the invocation semantics of existing `list --query`, `ready --query`, or `weave --pattern`.

## CDP-PROP-001.P4 Proposed scope

- **CDP-PROP-001.S1:** Add `query list` (registered query metadata ordered by name) and `query explain <name>` (caller guidance for params and definition), mirroring `pattern list` / `pattern explain`.
- **CDP-PROP-001.S2:** Add read-only JSON socket allowlist operations for query introspection (e.g. `query-list`, `query-explain`), narrowing SPEC-004.C27's exclusion to registry *mutation* only, so query listing/inspection becomes allowed read-only work consistent with `pattern-list` / `pattern-explain`.
- **CDP-PROP-001.S3:** Make the named-definition command family symmetric and self-describing in help text: `query`/`pattern` both offer `list`/`explain`, and the read/write application split is explicit.
- **CDP-PROP-001.S4:** Add spec/README language framing `weave` as the CLI-safe front door over the same transactional engine as REPL-only `batch`.
- **CDP-PROP-001.S5:** Update root specs via feature-local deltas at plan time: SPEC-002 (new CLI commands, plus SPEC-002.C24 so `query list`/`query explain` are read-only commands not gated by received-payload hooks, matching `pattern list`/`explain`) and SPEC-004 (allowlist). SPEC-003 today offers only partial query introspection — `queries` lists the registry (SPEC-003.C11) and `query` executes rather than explains (SPEC-003.C13) — so decide whether REPL/API parity needs a new inspect/explain helper or whether explain stays CLI/socket-only.

## CDP-PROP-001.P5 Open questions

- **CDP-PROP-001.Q1:** Scope confirmation — "align patterns and weaves" is read here as aligning the named-definition CLI family (queries ↔ patterns, with `weave` as the pattern applier). Confirm query parity is in scope (recommended) rather than a pattern/weave-only change.
- **CDP-PROP-001.Q2:** Invocation uniformity — keep `list --query` / `weave --pattern` as-is (recommended), or add symmetric group verbs (e.g. `pattern weave`, `query run`)? Avoid over-growing the CLI.
- **CDP-PROP-001.Q3:** `query explain` payload — reuse the pattern-explain guidance shape (spec form, required/optional keys) or a query-specific shape (params, DSL summary)?
- **CDP-PROP-001.Q4:** Record as an RFC before planning, since CDP-PROP-001.S2 reverses the deliberate SPEC-004.C27 exclusion of query listing from the socket allowlist?
