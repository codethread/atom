# Task 4: Move weaver storage to XDG state

**Document ID:** `TASK-MillRouterRuntime-004`

## TASK-MillRouterRuntime-004.P1 Scope

Type: AFK

Update the Clojure weaver runtime so mill-supplied state/data dirs are authoritative and startup prepares storage before publishing ready metadata.

## TASK-MillRouterRuntime-004.P2 Must implement exactly

- **TASK-MillRouterRuntime-004.MI1:** Extend `skein.weaver.runtime` command-line parsing to require explicit `--config-dir`, `--state-dir`, and `--data-dir` values; startup must fail loudly when any required dir input is absent.
- **TASK-MillRouterRuntime-004.MI2:** Update `skein.weaver.config/world` or equivalent construction so selected config-dir, state-dir, and data-dir are independent inputs rather than always derived from config-dir.
- **TASK-MillRouterRuntime-004.MI3:** Publish `weaver.json`, `weaver.edn`, and `weaver.sock` under the selected state-dir supplied by mill.
- **TASK-MillRouterRuntime-004.MI4:** Store default SQLite data under the selected data-dir supplied by mill.
- **TASK-MillRouterRuntime-004.MI5:** Initialize or validate the SQLite schema during weaver startup before ready metadata is published. Startup must fail loudly on malformed/incompatible storage.
- **TASK-MillRouterRuntime-004.MI6:** Remove public JSON-socket dependency on a database `init` operation for normal CLI use while preserving any trusted REPL/test helper if still needed.
- **TASK-MillRouterRuntime-004.MI7:** Ensure startup config still loads `init.clj` then `init.local.clj` from selected config-dir, not XDG state.

## TASK-MillRouterRuntime-004.P3 Done when

- **TASK-MillRouterRuntime-004.DW1:** Clojure tests show a weaver world can use config files from one directory and state/data from separate temp directories.
- **TASK-MillRouterRuntime-004.DW2:** Clojure tests show empty/missing DB schema is ready after weaver startup without a client init call.
- **TASK-MillRouterRuntime-004.DW3:** Tests show runtime metadata records the selected config-dir plus XDG state/data/database paths.
- **TASK-MillRouterRuntime-004.DW4:** Tests show startup fails clearly when `--config-dir`, `--state-dir`, or `--data-dir` is missing.
- **TASK-MillRouterRuntime-004.DW5:** Tests show present failing `init.clj` still aborts startup before ready metadata.
- **TASK-MillRouterRuntime-004.DW6:** `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` passes for touched Clojure tests.

## TASK-MillRouterRuntime-004.P4 Out of scope

- **TASK-MillRouterRuntime-004.OS1:** Go operation forwarding for ordinary strand commands.
- **TASK-MillRouterRuntime-004.OS2:** REPL helper attachment changes.
- **TASK-MillRouterRuntime-004.OS3:** Data migration from old `.skein/data` locations.

## TASK-MillRouterRuntime-004.P5 References

- **TASK-MillRouterRuntime-004.REF1:** [Weaver Runtime delta](../specs/daemon-runtime.delta.md)
- **TASK-MillRouterRuntime-004.REF2:** `src/skein/weaver/config.clj`, `src/skein/weaver/runtime.clj`, `src/skein/weaver/metadata.clj`, `src/skein/db.clj`
