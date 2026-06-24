# Todo Graph MVP

A small Clojure todo graph tool using `next.jdbc` + SQLite.

- `tasks.attributes` is JSON stored as `TEXT` and validated with SQLite JSON1.
- `task_edges` stores graph relationships such as `depends-on` and `mentions`.
- Runtime/userland attributes are open-ended for now; schemas can be added later.

## Agent quickstart

Use the CLI first for automation. Pick a disposable database path and pass it with every command:

```sh
DB=/tmp/todo-agent.sqlite
clojure -M:todo --db "$DB" init
clojure -M:todo --db "$DB" add design "Sketch model" --attr status=done --attr priority=high
clojure -M:todo --db "$DB" add docs "Write docs" --attr status=todo --attr owner=agent
clojure -M:todo --db "$DB" link docs design depends-on --attr reason="docs follow design"
clojure -M:todo --db "$DB" --format edn ready
clojure -M:todo --db "$DB" --format json by-attr owner agent
clojure -M:todo --db "$DB" --format edn deps docs
clojure -M:todo --db "$DB" done docs
```

### CLI command vocabulary

Global options:

- `--db <path>`: SQLite database path. Defaults to `todo.sqlite`.
- `--format human|edn|json`: output mode for query commands. Use `edn` for Clojure-native automation and `json` for general shell automation.
- `--attr key=value`: repeatable task or edge attribute. CLI attribute values are stored as strings.

Commands:

- `init`: create the schema in the selected database.
- `add <id> <title> [--attr key=value ...]`: create a task.
- `link <from-id> <to-id> <edge-type> [--attr key=value ...]`: create a graph edge.
- `show <id>`: fetch one task.
- `list`: list all tasks.
- `deps <id>`: list direct `depends-on` dependencies for a task.
- `transitive-deps <id>`: list all recursive `depends-on` dependencies for a task.
- `blocking <id>`: list tasks directly blocked by a task.
- `ready`: list non-done tasks whose direct dependencies are all `done`.
- `by-attr <key> <value>`: query tasks by a top-level JSON1 attribute.
- `done <id>`: set the task's conventional `status` attribute to `done`.

## Agent REPL helpers

Use `todo.repl` for interactive exploration when a REPL is already available:

```clojure
(require '[todo.repl :refer :all])
(open! "agent.sqlite")
(init!)
(task! "design" "Sketch model" {:status "done" :priority "high"})
(task! "docs" "Write docs" {:status "todo" :owner "agent"})
(depends! "docs" "design")
(ready)
(by-attr :owner "agent")
(deps "docs")
(done! "docs")
```

Helpers use the datasource opened with `open!`; calling database helpers first fails with a clear error.

REPL helper vocabulary:

- `open!`: select the active SQLite database.
- `init!`: create the schema in the active database.
- `task!`: create a task.
- `edge!`: create any edge type.
- `depends!`: create a conventional `depends-on` edge.
- `done!`: set the conventional `status` attribute to `done`.
- `tasks`: list all tasks.
- `task`: fetch one task.
- `deps`: list direct `depends-on` dependencies.
- `transitive-deps`: list recursive `depends-on` dependencies.
- `blocking`: list tasks directly blocked by a task.
- `ready`: list non-done tasks whose direct dependencies are all `done`.
- `by-attr`: query tasks by a top-level JSON1 attribute.
- `graph`: list all edges touching a task.

## MVP conventions

Supported task attributes remain open-ended JSON. The MVP establishes these conventional keys for interoperability:

- `status`: task state. `done` means complete; non-`done` values such as `todo` and `doing` remain active.
- `priority`: free-form priority label such as `high`, `medium`, or `low`.
- `due-date`: ISO-like date string used by the demo queries.
- Other attributes, such as `owner`, `reason`, or `estimate-hours`, are userland values and can be queried with `by-attr` when stored on tasks.

Supported edge types are plain strings. The MVP conventions are:

- `depends-on`: `from` cannot be ready until `to` is `done`.
- `mentions`: loose reference edge with no readiness semantics.

## Run the smoke demo

```sh
clojure -M:smoke
```

This recreates `smoke.sqlite`, exercises the CLI workflow, exercises the REPL helpers, and prints example JSON1 and graph queries.

## Run the TUI

The TUI remains available for local human editing, but agents should prefer the CLI or REPL helpers above.

```sh
clojure -M:run
# or choose a database file
clojure -M:run my-todos.sqlite
```

Attribute input in the TUI is a simple comma-separated format:

```text
priority=high,due-date=2026-07-01,status=todo
```

## Schema

```sql
CREATE TABLE tasks (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  attributes TEXT NOT NULL DEFAULT '{}',
  CHECK (json_valid(attributes))
);

CREATE TABLE task_edges (
  from_task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  to_task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  edge_type TEXT NOT NULL,
  attributes TEXT NOT NULL DEFAULT '{}',
  PRIMARY KEY (from_task_id, to_task_id, edge_type),
  CHECK (json_valid(attributes))
);
```
