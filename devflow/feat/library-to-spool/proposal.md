# Library to Spool Proposal

**Document ID:** `LTS-PROP-001`
**Last Updated:** 2026-07-01
**Related RFCs:** None (direction from in-session naming review; see LTS-PROP-001.Q2)
**Related root specs:** [REPL API](../../specs/repl-api.md), [Weaver Runtime](../../specs/daemon-runtime.md), [CLI Surface](../../specs/cli.md)

## LTS-PROP-001.P1 Problem

Now the API surface is stable, "library" is the one runtime noun that actively misleads. It does double duty in Skein's own specs: (a) the generic Clojure/Maven sense — `libs.edn` literally lists Clojure library coordinates with `:local/root` paths, and the specs discuss "Maven/remote dependency coordinates" — and (b) Skein's specific concept: a unit of trusted code you make available (`sync!`) and then activate (`use!`) inside a weaver. Naming Skein's concept "library" invites exactly the assumptions Skein disclaims (versions, registries, remote fetch), and it collides with the generic term used in the same sentences.

The textile metaphor absorbs this concept cleanly: a **spool** is thread wound and ready to feed the loom. That maps onto Skein's two-phase model — `sync!` winds the spool onto the creel (available), `use!` threads it through (activated) — and disambiguates Skein's unit from a plain Clojure library.

There is also no lightweight discovery convention for spool roots today. A future package manager (out of scope here) benefits from a filesystem-visible marker existing now, before conventions harden.

This scope is deliberately narrow: the in-session naming review concluded that `weaver`, `world`, `query`, `batch`, `pattern`, `weave`, and `mill` should stay (see LTS-PROP-001.NG1). `library → spool` is the single rename that removes a real standing ambiguity rather than a lateral relabel.

## LTS-PROP-001.P2 Goals

- **LTS-PROP-001.G1:** Rename the user-facing concept from "library" to "spool" across README, docs, CLI help text, and spec prose wherever the text names Skein's trusted-code-unit concept.
- **LTS-PROP-001.G2:** Rename the approved-config surface: `libs.edn`/`libs.local.edn` → `spools.edn`/`spools.local.edn` and top-level `:libs` → `:spools`, while keeping each entry's Clojure coordinate shape (`{coord {:local/root path}}`) unchanged.
- **LTS-PROP-001.G3:** Introduce a recommended, non-enforced `.spool` directory-suffix naming convention for spool roots (e.g. `spools/my-module.spool`) as a discovery marker a future package manager can build on.
- **LTS-PROP-001.G4:** Keep generic runtime verbs and the loader namespace unchanged (`skein.runtime.alpha` with `sync!`, `use!`, `reload!`, `approved`, `syncs`, `uses`), consistent with the project's verb-vs-noun rename rule (`SR-PLAN-001.A3`).
- **LTS-PROP-001.G5:** Preserve all runtime-library behavior and semantics; this is a vocabulary and convention change, not a runtime-model change.
- **LTS-PROP-001.G6:** Drop old names outright under TEN-000 — no `libs.edn`/`:libs` fallback, alias, or dual-read path.

## LTS-PROP-001.P3 Non-goals

- **LTS-PROP-001.NG1:** No rename of `weaver`, `world`, `query`, `batch`, `pattern`, `weave`, or `mill`. The naming review kept these: `weaver` is the deliberately chosen active-engine noun (RFC-006), `world` is plumbing, `query`/`batch` stay generic per `SR-PLAN-001.A3`, and `pattern`/`weave` already read well.
- **LTS-PROP-001.NG2:** No package registry, remote/Maven fetch, dependency solver, lockfile, or `strand spool` CLI command. The `.spool` convention is discovery groundwork only, not a package manager.
- **LTS-PROP-001.NG3:** No enforcement or validation of the `.spool` suffix. Unsuffixed spool roots keep working; the suffix stays a recommended convention.
- **LTS-PROP-001.NG4:** No change to runtime-library semantics: sync/use/reload behavior, the shared/local overlay, precedence, and local-root resolution are unchanged.
- **LTS-PROP-001.NG5:** No rename of the generic Clojure per-entry `:local/root` key, which mirrors the Clojure deps coordinate convention.

## LTS-PROP-001.P4 Proposed scope

- **LTS-PROP-001.S1:** Replace "library/libraries" with "spool/spools" where the text names Skein's trusted-code-unit concept (README, `docs/skein.md`, getting-started, any library-authoring doc, CLI help, and spec prose). Keep "Clojure library" where it genuinely means a generic Clojure coordinate.
- **LTS-PROP-001.S2:** Rename the config surface: `libs.edn` → `spools.edn`, `libs.local.edn` → `spools.local.edn`, top-level `:libs` → `:spools`. `strand init` bootstrap creates `spools.edn` and a `spools/` directory (was `libs/`); repo `.gitignore` ignores `spools.local.edn`. Overlay and precedence semantics are unchanged.
- **LTS-PROP-001.S3:** Document the `.spool` naming convention: spool roots should be named `<name>.spool` (e.g. `spools/my-module.spool`), and `:local/root` may point at such a directory. State the forward-looking rationale — a future package manager can enumerate spools by scanning for `*.spool` — and that it is non-enforced.
- **LTS-PROP-001.S4:** Keep `skein.runtime.alpha` and the blessed `skein.*.alpha` verb/API names as-is.
- **LTS-PROP-001.S5:** Update affected root specs via feature-local deltas at plan time: CLI init bootstrap (SPEC-002), REPL API runtime library workspace helpers (SPEC-003.P5), and Weaver Runtime runtime library workspace model (SPEC-004.P9).
- **LTS-PROP-001.S6:** Coordinate with the active `library-author-testing-support` feature, which also uses `libs`/library vocabulary, so spec deltas and docs do not conflict.

## LTS-PROP-001.P5 Open questions

- **LTS-PROP-001.Q1:** Rename depth — do we also rename the compatibility-only `skein.libs.*` namespace family and any `libs`-derived internal identifiers to spool, or keep the rename at concept/config/docs level? Recommendation: keep `skein.runtime.alpha` and the verbs; treat a deep namespace rename as optional/deferred.
- **LTS-PROP-001.Q2:** Should this ship a full RFC-006-style RFC before planning, given it records the "spool, not bobbin/cone/skein" choice and the explicit decision to leave `weaver`/`world`/`query` alone? The in-session review already produced the alternatives analysis.
- **LTS-PROP-001.Q3:** Sequencing versus `library-author-testing-support` (which adds `skein.test.alpha` and references `libs.edn`): rename before, after, or jointly with that feature?
- **LTS-PROP-001.Q4:** Exact `.spool` convention shape — suffix on the directory (`my-module.spool/`) versus a marker file inside the root — and whether `spools.edn` entries reference the suffixed directory directly.
