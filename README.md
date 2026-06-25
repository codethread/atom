# Todo Graph MVP

A small Clojure todo graph tool using `next.jdbc` + SQLite.

It exists to give coding agents and humans a lightweight local task graph:

- tasks are stored in SQLite;
- tasks have first-class `status`, `created_at`, `updated_at`, and `final_at` lifecycle fields;
- open-ended task attributes are JSON stored as `TEXT` and queried with SQLite JSON1;
- `task_edges` stores acyclic graph relationships used by task updates, including `depends-on` for readiness;
- agents can use a stripped scriptable CLI or compact REPL API.

For contributor, debugging, and implementation guidance, see [AGENTS.md](./AGENTS.md). For durable behavior contracts, see the [Devflow spec index](./devflow/README.md#root-specs); for the daemon/REPL/CLI design mental model, see [Devflow Philosophy](./devflow/PHILOSOPHY.md).

## Requirements

- Clojure CLI
- Java / OpenJDK
- SQLite, provided by the `org.xerial/sqlite-jdbc` dependency at runtime

On this system, Homebrew OpenJDK may need to be put on PATH:

```sh
PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke
```

## Quickstart

Run the unit tests and smoke demo:

```sh
clojure -M:test
(cd cli && go test ./...)
clojure -M:smoke
```

The smoke path builds and removes `./cli/bin/todo` while exercising the Go CLI against the daemon JSON socket.

Build and use the daemon-backed Go CLI. The default daemon world reads `~/.config/atom/config.json`; create it with an absolute `source` path to this checkout:

```sh
go build -o ./cli/bin/todo ./cli/cmd/todo
TODO="$PWD/cli/bin/todo"
mkdir -p ~/.config/atom
printf '{"source":"%s","format":"human"}\n' "$PWD" > ~/.config/atom/config.json
"$TODO" daemon start
```

Run task commands from another terminal and any working directory:

```sh
"$TODO" daemon status
"$TODO" init
design=$("$TODO" add "Sketch model" --status done --attr priority=high)
docs=$("$TODO" add "Write docs" --attr owner=agent)
"$TODO" update "$docs" --edge depends-on:$design
"$TODO" --format json ready
"$TODO" daemon stop
```

Use `--config-dir <dir>` for a disposable alternate daemon world; its config lives in `<dir>/config.json`, runtime state in `<dir>/state`, and data in `<dir>/data`.

Use the connected helper REPL against a running daemon:

```sh
"$TODO" daemon repl
```

```clojure
(init!)
(def design (:id (task! "Sketch model" "done" {:priority "high"})))
(def docs (:id (task! "Write docs" {:owner "agent"})))
(update! docs {:edges [{:type "depends-on" :to design}]})
(defquery! 'agent-owned '[:= [:attr :owner] "agent"])
(ready)
```

Agents can run trusted non-interactive forms through the same connected context. `--stdin` prints direct Clojure results, one per top-level form, with no CLI response envelope:

```sh
printf '(ready)\n' | "$TODO" daemon repl --stdin
```

Named queries live in daemon memory for the current daemon lifetime. Load them from trusted `init.clj` in the selected config-dir or REPL helpers such as `defquery!` / `load-queries!`, then consume them from either REPL helpers or the small CLI surface:

```sh
"$TODO" --format json list --query agent-owned
"$TODO" daemon stop
```

The registry is not saved to SQLite; restart the daemon and reload trusted config/REPL query definitions when needed. The CLI intentionally has no `--query-file` loader so runtime customization stays daemon/REPL-owned, matching the daemon-core design described in [Devflow Philosophy](./devflow/PHILOSOPHY.md).

## Runtime libraries and startup config

Atom is not core-only: trusted runtime customization belongs in the selected daemon world's `init.clj` and connected REPL workflows. The selected `--config-dir` is also a user-owned library workspace. Keep the Atom checkout wherever `config.json` `source` points, then keep user/community source under the config-dir by Git repo, Git submodule, or manual copy. Atom does not clone source, install packages, or expose plugin/package/library activation commands in the public CLI.

The blessed extension path is normal Clojure libraries: approve local roots in `libs.edn`, sync them into the daemon, then activate optional modules with `atom.libs.alpha/use!`.

```clojure
;; <config-dir>/libs.edn
{:libs {community/graph {:local/root "libs/community-graph"}
        my/config       {:local/root "/absolute/path/to/my-config-lib"}}}
```

Relative roots resolve against the selected config-dir; absolute roots are explicit user-approved paths. Roots are canonicalized, so symlinks and relative segments normalize before sync. Each local root should be a tools.deps project root, for example with its own `deps.edn` and `src` directory.

A resilient `init.clj` can be mostly layered `use!` calls:

```clojure
(require '[atom.libs.alpha :as libs])

(libs/sync!)

(libs/use! :graph
  {:ns 'community.graph.alpha
   :libs #{'community/graph}
   :call 'community.graph.alpha/install!})

(libs/use! :my/config
  {:ns 'my.config.alpha
   :libs #{'my/config}
   :after [:graph]
   :call 'my.config.alpha/install!})
```

`use!` records loaded, skipped, and failed attempts for fix-forward inspection. Optional modules with missing roots or unmet `:after` gates skip without bricking daemon startup; malformed options and strict raw `require` still fail loudly when that is what you want.

Inspect approved libraries, sync outcomes, and module-use state through the connected REPL or non-interactive stdin:

```sh
printf '(require '\''[atom.libs.alpha :as libs])\n(libs/approved)\n(libs/sync!)\n(libs/syncs)\n(libs/uses)\n' | "$TODO" daemon repl --stdin
```

REPL process boundaries matter: `libs/sync!` mutates the daemon JVM classpath, and `libs/use!` runs daemon-side activation. A direct `require` typed into a connected helper REPL uses that helper JVM's classpath, not newly synced daemon libraries. Use daemon-routed helpers for daemon-side activation, or put required daemon startup code in `init.clj`.

Coupling tiers are explicit. `atom.libs.alpha` is the documented, tested path for library-workspace startup and REPL workflows. Supported lower-level libraries are available to trusted code when the coupling cost is worth it. Internal implementation namespaces are inspectable and callable, but may change freely. Raw SQLite/schema access is also allowed for trusted code, with the caller owning compatibility risk when persistence details change.

## Data model

The durable data contract is specified in [Task Model](./devflow/specs/task-model.md). At a high level:

- tasks have a generated unique text id, title, status, lifecycle timestamps, and open-ended JSON object attributes;
- final statuses are `done`, `failed`, and `cancelled`, and set `final_at`;
- task edges connect two tasks with a canonical edge type and open-ended JSON object attributes;
- edge writes reject unsupported types and directed cycles;
- `depends-on` edges define readiness semantics.

## Development

See:

- [AGENTS.md](./AGENTS.md) for contributor/debug/build guidance;
- [Task Model](./devflow/specs/task-model.md) for data semantics;
- [CLI Surface](./devflow/specs/cli.md) for full command vocabulary;
- [REPL API](./devflow/specs/repl-api.md) for full helper vocabulary.
