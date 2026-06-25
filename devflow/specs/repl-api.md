# REPL API

**Document ID:** `SPEC-003`
**Status:** Implemented
**Last Updated:** 2026-06-25
**Code:** `src/todo/repl.clj`

## SPEC-003.P1 Purpose

The REPL API gives coding agents and human developers a compact interactive Clojure interface over the stripped task surface.

## SPEC-003.P2 Interface

Helpers:

```clojure
open!
init!
task!
update!
task
tasks
ready
```

## SPEC-003.P3 Contracts

- **SPEC-003.C1:** `open!` selects one active daemon-backed database connection by database path. Helpers that need a daemon fail before `open!`, and daemon/transport failures surface loudly as Clojure exceptions.
- **SPEC-003.C2:** `init!` initializes the active database schema.
- **SPEC-003.C3:** `task!` creates a task and returns the created row. Supported arities are `(task! title)`, `(task! title attributes)`, and `(task! title status attributes)`.
- **SPEC-003.C4:** `update!` accepts a task id and patch map with optional `:title`, `:status`, `:attributes`, and `:edges`.
- **SPEC-003.C5:** `:edges` are maps with `:type`, `:to`, and optional `:attributes`; each edge is written from the updated task to `:to`.
- **SPEC-003.C6:** `task`, `tasks`, and `ready` return rows with JSON-bearing columns normalized to Clojure values.
- **SPEC-003.C7:** `ready` returns non-final tasks whose direct `depends-on` dependencies are all final.

## SPEC-003.P4 Non-goals

The REPL API does not expose bespoke helpers such as `depends!`, `edge!`, `done!`, `by-attr`, `deps`, `blocking`, or `graph`. Those are either covered by `update!` or deferred to future query/inspection design.
