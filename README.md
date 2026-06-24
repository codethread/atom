# Todo Graph MVP

A small Clojure TUI todo app using `next.jdbc` + SQLite.

- `tasks.attributes` is JSON stored as `TEXT` and validated with SQLite JSON1 (`json_valid`).
- `task_edges` stores graph relationships such as `depends-on` and `mentions`.
- Runtime/userland attributes are open-ended for now; schemas can be added later.

## Run the smoke demo

```sh
clojure -M:smoke
```

This recreates `smoke.sqlite`, inserts tasks and dependency edges, then prints example JSON1 and graph queries.

## Run the TUI

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
