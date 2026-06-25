---
name: atom
description: Query and operate the Atom todo task database from agents, scripts, and the REPL.
---

# Atom task queries

Atom supports a small EDN query DSL for filtering tasks. Use it with `list` and `ready`, or load named queries in the REPL.

## CLI

The public CLI consumes named queries already loaded into daemon memory. Start the daemon for the selected config-dir world before running task/query commands.

```sh
todo --format json ready \
  --query owned-open \
  --param owner=agent
```

Use `--config-dir <dir>` for a disposable or feature-local daemon world:

```sh
todo --config-dir "$world" --format json list --query owned-open --param owner=agent
```

## REPL

Use the connected helper REPL to load or define queries in the selected daemon world:

```sh
todo daemon repl
```

```clojure
(load-queries! "queries.edn")
(query 'owned-open {:owner "agent"})
(ready 'owned-open {:owner "agent"})
```

Agents can do the same non-interactively through direct Clojure stdin output:

```sh
printf '(load-queries! "queries.edn")\n(ready '\''owned-open {:owner "agent"})\n' | todo daemon repl --stdin
```

Define a query at runtime:

```clojure
(defquery! 'high-priority [:= [:attr :priority] "high"])
(query 'high-priority)
```

## Query forms

Queries are EDN vectors. Supported fields:

- `:id`
- `:title`
- `:status`
- `:created_at`
- `:updated_at`
- `:final_at`
- `[:attr :key]` for JSON task attributes
- `[:attr :nested :key]` for nested JSON attributes

Supported operators:

```clojure
[:= field value]
[:!= field value]
[:< field value]
[:<= field value]
[:> field value]
[:>= field value]
[:in field [value ...]]
[:exists field]
[:missing field]
[:and query ...]
[:or query ...]
[:not query]
```

Use `[:param :name]` inside named queries to accept runtime values.

```clojure
{:params [:owner :priority]
 :where [:and
         [:= [:attr :owner] [:param :owner]]
         [:= [:attr :priority] [:param :priority]]]}
```

CLI `--param` values are strings. REPL parameter values may be Clojure scalars that SQLite JSON comparisons can handle.
