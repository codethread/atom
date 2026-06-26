# Add atom.test.alpha

## TASK-004.P1 Scope

Type: AFK

Add the blessed author-side test helper namespace at `src/atom/test/alpha.clj`. It should provide the minimal daemon-world lifecycle API for idiomatic `clojure.test`, with explicit storage selection for both file-backed and in-memory SQLite.

References:

- [Plan](../library-author-testing-support.plan.md) `LAT-PLAN-001.PH4`
- [REPL API delta](../specs/repl-api.delta.md)
- [API shape spike](../../../spikes/2026-06-26-atom-test-alpha-api.md)
- [Classpath spike](../../../spikes/2026-06-26-library-author-classpath.md)

## TASK-004.P2 Implementation notes

- Add `src/atom/test/alpha.clj` so external library test JVMs can require it through an Atom `:local/root` dependency.
- Implement a small API with names from the delta unless implementation proves a better minimal naming:
  - `with-daemon-world`
  - `daemon-world-fixture`
  - `repl!`
- The helper should:
  - create short-path isolated config dirs by default
  - write `config.json`, `libs.edn`, `init.clj`, and config-dir-relative fixture files from options
  - start an in-process daemon runtime
  - expose a context map with config/state/data dirs, source checkout, storage kind, metadata, and runtime handle
  - stop the daemon and clean up in `finally`
  - fail loudly on startup, init, eval, stop, or cleanup errors
- `repl!` should evaluate daemon-routed forms and return Clojure data or throw with useful context.
- Support at least `:storage :sqlite-file` and `:storage :sqlite-memory`.
- Do not add task wrappers, query wrappers, assertion DSLs, package install helpers, Go CLI subprocess helpers, or CLI binary discovery.
- Add tests for helper lifecycle, file fixture writing, daemon eval, cleanup, and both storage modes.

## TASK-004.P3 Done when

- `atom.test.alpha` can be required from the normal Atom source path.
- A test can load a fixture library through disposable `libs.edn`, call `libs/sync!`, and activate it through `libs/use!` via daemon-routed forms.
- Both file-backed and in-memory storage work through the same helper shape.
- Helper-generated worlds avoid long Unix socket paths where practical.

## TASK-004.P4 Validation

Run:

```sh
PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test
```

Include focused helper test output in the plan Developer Notes if full tests are not run.
