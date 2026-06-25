# REPL API

**Document ID:** `SPEC-003`
**Status:** Implemented
**Last Updated:** 2026-06-25
**Code:** `src/todo/repl.clj`

## SPEC-003.P1 Purpose

The REPL API gives coding agents and human developers a compact interactive Clojure interface over the stripped task surface and the selected daemon world.

## SPEC-003.P2 Interface

Helpers:

```clojure
connect!
init!
task!
update!
task
defquery!
load-queries!
queries
query
tasks
ready
```

## SPEC-003.P3 Contracts

- **SPEC-003.C1:** `connect!` selects one active daemon connection by daemon world. With no arguments it selects the default daemon world; with a config-dir argument it selects that explicit daemon world. It never accepts a database path.
- **SPEC-003.C2:** `todo daemon repl` preloads the REPL helper namespace, calls `connect!` for the selected daemon world, and presents the prompt.
- **SPEC-003.C3:** `todo daemon repl --stdin` uses the same preloaded, connected helper context, reads forms from stdin, evaluates them in order, prints one direct normal Clojure result per top-level form, and exits. Callers that want one machine-readable payload should wrap work in one top-level `do` or `let`.
- **SPEC-003.C4:** Helpers that need a daemon fail before connection with remediation that points to `todo daemon repl` or `connect!`; daemon/transport failures surface loudly as Clojure exceptions.
- **SPEC-003.C5:** `init!` initializes the active daemon store schema.
- **SPEC-003.C6:** `task!` creates a task and returns the created row. Supported arities are `(task! title)`, `(task! title attributes)`, and `(task! title status attributes)`.
- **SPEC-003.C7:** `update!` accepts a task id and patch map with optional `:title`, `:status`, `:attributes`, and `:edges`.
- **SPEC-003.C8:** `:edges` are maps with `:type`, `:to`, and optional `:attributes`; each edge is written from the updated task to `:to`.
- **SPEC-003.C9:** `defquery!` registers a named query expression or parameterized query map in the active daemon's in-memory query registry.
- **SPEC-003.C10:** `load-queries!` reads one EDN map of query names to query definitions and merges it into the active daemon's in-memory query registry.
- **SPEC-003.C11:** `queries` returns the active daemon's in-memory query registry.
- **SPEC-003.C12:** Query registry contents last only for the active daemon lifetime; reload trusted config or call `defquery!` / `load-queries!` again after daemon restart.
- **SPEC-003.C13:** `query` returns tasks matching an ad hoc query definition or daemon-registered query name, with optional runtime parameters.
- **SPEC-003.C14:** `task`, `tasks`, `query`, and `ready` return rows with JSON-bearing columns normalized to Clojure values.
- **SPEC-003.C15:** `ready` returns non-final tasks whose direct `depends-on` dependencies are all final and may be further filtered by an ad hoc or registered query.
- **SPEC-003.C16:** Blessed library-workspace helpers live in explicit `atom.*.alpha` namespaces, not in the preloaded `todo.repl` helper namespace.
- **SPEC-003.C17:** `atom.libs.alpha` exposes approved library config helpers, approved-local-root sync helpers, resilient module activation with `use!`, and daemon-lifetime sync/use introspection.
- **SPEC-003.C18:** `atom.libs.alpha` helpers route to the selected daemon world when called from connected REPL clients. Direct `require` in a connected helper REPL remains local to the helper JVM.
- **SPEC-003.C19:** `atom.prelude.alpha` is opt-in and favors `atom.libs.alpha` conveniences. Blessed alpha namespaces are the documented path, but trusted users may require lower-level namespaces or read raw SQLite when they accept compatibility cost.

## SPEC-003.P4 Example library init

A selected config-dir `init.clj` may sync approved local roots and activate optional modules:

```clojure
(require '[atom.libs.alpha :as libs])

(libs/sync!)
(libs/use! :my/module
  {:ns 'my.module.alpha
   :libs #{'my/module}
   :call 'my.module.alpha/install!})
```

A selected config-dir `libs.edn` approves local roots:

```clojure
{:libs {my/module {:local/root "libs/my-module"}}}
```

## SPEC-003.P5 Non-goals

The REPL API does not expose bespoke helpers such as `depends!`, `edge!`, `done!`, `by-attr`, `deps`, `blocking`, or `graph`. Those are either covered by `update!` or the generic query API.

The REPL API does not add CLI package authoring commands, package installation, source fetching, or plugin-directory loading.
