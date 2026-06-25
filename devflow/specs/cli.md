# CLI Surface

**Document ID:** `SPEC-002`
**Status:** Implemented
**Last Updated:** 2026-06-25
**Related RFCs:** [RFC-002 Task Query DSL](../rfcs/2026-06-24-task-query-dsl.md), [RFC-003 Fast JSON Socket CLI](../archive/26-06-25__go-cli-migration/rfcs/2026-06-25-fast-json-socket-cli.md), [RFC-004 Go CLI Migration](../archive/26-06-25__go-cli-migration/rfcs/2026-06-25-go-cli-migration.md)
**Code:** `cli/`, `src/todo/daemon`

## SPEC-002.P1 Purpose

The CLI is the primary scripted interface for coding agents. It exposes a deliberately small task surface: initialize storage, create tasks, update tasks, inspect tasks, list tasks, ask for ready work, and manage the local daemon runtime.

The public CLI is a thin Go executable named `todo`. It parses simple flags, resolves client defaults, sends JSON requests over the daemon's local Unix socket, formats human or JSON output, and never opens SQLite or evaluates rich query definitions locally.

## SPEC-002.P2 Interface

Entrypoint:

```text
todo [--db <path>] [--client-config <path>] [--format human|json] <command> [args]
```

Commands:

```text
init
add <title> [--status todo|done|failed|cancelled] [--attr key=value ...]
update <id> [--title title] [--status todo|done|failed|cancelled] [--attr key=value ...] [--edge edge-type:to-id ...]
show <id>
list [--query name] [--param key=value ...]
ready [--query name] [--param key=value ...]
daemon start [--config <path>]
daemon stop
daemon status
```

## SPEC-002.P3 Contracts

- **SPEC-002.C1:** `--db` selects the daemon/database runtime identity and defaults to `todo.sqlite`; task commands require a matching reachable daemon and do not silently open SQLite directly.
- **SPEC-002.C2:** `--client-config` selects a JSON client config file for low-privilege defaults. Without it, the CLI looks under `$XDG_CONFIG_HOME/todo/config.json` or `~/.config/todo/config.json`. Explicit CLI flags override config values.
- **SPEC-002.C3:** Client config may contain only supported client defaults such as `db` and `format`; malformed config, unsupported keys, or wrong value types fail non-zero.
- **SPEC-002.C4:** `--format` accepts `human` or `json` and defaults to `human`. EDN is not a public CLI output format.
- **SPEC-002.C5:** `add` creates a task with generated id, first-class status, timestamps, and string-valued CLI attributes.
- **SPEC-002.C6:** `update` patches title, status, attributes, and task edges for one existing task.
- **SPEC-002.C7:** `--edge edge-type:to-id` creates or updates an outgoing edge from the updated task to the target task.
- **SPEC-002.C8:** `show`, `list`, and `ready` return task rows with normalized `attributes` in JSON output.
- **SPEC-002.C9:** `ready` returns non-final tasks whose direct `depends-on` dependencies are all final.
- **SPEC-002.C10:** `list` and `ready` accept an optional named query from daemon memory with `--query` and repeated string-valued `--param key=value` runtime parameters.
- **SPEC-002.C11:** `--where` and `--format edn` are not part of the public Go CLI. Rich EDN query authoring belongs in trusted daemon config and REPL workflows.
- **SPEC-002.C12:** The CLI has no query registry mutation/listing commands and does not accept `--query-file`; query loading is a trusted daemon config or REPL workflow, and registry contents last only for the daemon lifetime.
- **SPEC-002.C13:** Malformed options, invalid statuses, invalid edge targets, unknown commands, stale/missing metadata, socket transport/identity failures, malformed daemon responses, and database/domain errors fail non-zero.
- **SPEC-002.C14:** `daemon start`, `daemon stop`, and `daemon status` manage the local daemon lifecycle for the selected database. `daemon start --config <path>` forwards trusted startup config to the Clojure daemon and stays in the foreground. `daemon status` validates metadata and socket identity and reports health, canonical database path, pid, daemon identity, socket endpoint, and nREPL endpoint. `daemon stop` stops only the matched daemon over the socket and waits for runtime metadata/socket cleanup.

## SPEC-002.P4 Deferred

`by-attr`, bespoke dependency inspection commands, `link`, `done`, `batch`, public CLI EDN query expressions, and query registry mutation commands are not part of the stripped public CLI.

The legacy `clojure -M:todo` entrypoint may remain available as an internal Clojure/dev support path, but it is not the public scripted CLI contract.
