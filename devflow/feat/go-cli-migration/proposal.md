# Go CLI Migration Proposal

**Document ID:** `PROP-001`
**Status:** Draft
**Last Updated:** 2026-06-25
**Related RFCs:** [RFC-003 Fast JSON Socket CLI](../../rfcs/2026-06-25-fast-json-socket-cli.md), [RFC-004 Go CLI Migration](../../rfcs/2026-06-25-go-cli-migration.md)
**Related root specs:** [CLI Surface](../../specs/cli.md), [Daemon Runtime](../../specs/daemon-runtime.md), [Project Tenets](../../TENETS.md)

## PROP-001.P1 Problem

The CLI is deliberately thin, but invoking it through `clojure -M:todo` still starts a JVM for every command. Once daemon query registry work lands, the daemon will own the task/query semantics needed by scripts, making the CLI an ideal candidate for migration to a small native client that talks to the daemon over a local Unix socket.

## PROP-001.P2 Goals

- **PROP-001.G1:** Replace the JVM-backed scripted CLI path with a Go executable while preserving the existing command vocabulary and simplifying public CLI data exchange to JSON plus human output.
- **PROP-001.G2:** Keep all task storage, query registry execution, and runtime customization in the daemon.
- **PROP-001.G3:** Add deterministic daemon discovery through runtime metadata that includes a Unix socket endpoint.
- **PROP-001.G4:** Add a simple XDG-based JSON user config for low-privilege client defaults such as database path and output format.
- **PROP-001.G5:** Fail loudly for missing daemons, stale metadata, socket errors, malformed config, and daemon/domain errors.

## PROP-001.P3 Non-goals

- **PROP-001.NG1:** Reimplementing SQLite persistence or task/query semantics in Go is out of scope.
- **PROP-001.NG2:** Removing nREPL, REPL helpers, or trusted daemon Clojure config is out of scope.
- **PROP-001.NG3:** Remote daemon access, authentication, and multi-user operation are out of scope.
- **PROP-001.NG4:** Durable saved-query persistence is out of scope; this feature assumes active query registry deltas `SPEC-002-D002` and `SPEC-004-D001` have been promoted or otherwise stabilized.
- **PROP-001.NG5:** A detailed implementation plan is out of scope until the active query registry contracts named above are stable.

## PROP-001.P4 Proposed scope

- **PROP-001.S1:** Change the documented CLI entrypoint to a native `todo` executable that supports the current thin command surface.
- **PROP-001.S2:** Route task and query commands through a JSON request/response protocol over a Unix domain socket advertised by daemon runtime metadata.
- **PROP-001.S3:** Preserve daemon lifecycle commands as the way to start, stop, and inspect the selected daemon/runtime identity.
- **PROP-001.S4:** Add XDG JSON config discovery for client defaults, with explicit CLI flags continuing to override config defaults.
- **PROP-001.S5:** Keep daemon startup trusted config separate from client config: global `--client-config` selects low-privilege CLI defaults, while `daemon start --config` remains trusted Clojure daemon startup config.
- **PROP-001.S6:** Remove EDN from the public CLI surface. The Go CLI supports `human` and `json` output only; EDN query authoring and inspection remain for Clojure REPL and trusted daemon config workflows.
- **PROP-001.S7:** Promote the CLI/daemon split into project tenets: the CLI is the thin JSON control surface, while the daemon/REPL is the rich semantic and inspection surface.
- **PROP-001.S8:** Use idiomatic Go libraries deliberately: prefer the standard library for Unix socket transport, JSON, contexts, errors, and tests; use Cobra for command/subcommand parsing if it keeps the command tree clearer than hand-rolled parsing; use a small XDG helper for config path discovery.
- **PROP-001.S9:** Stage contract changes now as spec deltas, then write the implementation plan after active query registry deltas `SPEC-002-D002` and `SPEC-004-D001` have been promoted or otherwise stabilized.

## PROP-001.P5 Open questions

- **PROP-001.Q1:** Decide whether daemon lifecycle commands in the Go CLI directly exec the Clojure daemon entrypoint or are packaged through a small launcher strategy.
