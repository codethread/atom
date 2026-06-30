# Task 6: Finalize specs and validation

**Document ID:** `TASK-MOS-006`

## TASK-MOS-006.P1 Scope

Type: AFK

Bring specs, examples, and validation into alignment with the implemented repository-scoped default weaver and mill-owned source resolution contract.

## TASK-MOS-006.P2 Must implement exactly

- **TASK-MOS-006.MI1:** Update root specs `devflow/specs/cli.md` and `devflow/specs/daemon-runtime.md` with the shipped contract from feature deltas when implementation is complete.
- **TASK-MOS-006.MI2:** Mark feature-local deltas as merged only if their durable content has been promoted into the root specs; otherwise leave them Reviewed and note why.
- **TASK-MOS-006.MI3:** Update user-facing docs or command help touched by this feature so they no longer instruct users to persist source in `config.json` and accurately describe default repository-scoped selection.
- **TASK-MOS-006.MI4:** Run primary validation relevant to touched areas: `(cd cli && go test ./...)`, `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:smoke`, and `PATH="/opt/homebrew/opt/openjdk/bin:$PATH" clojure -M:test` if Clojure runtime/smoke code changed.
- **TASK-MOS-006.MI5:** Record validation results and any cut/deferred scope in `PLAN-MOS-001.P9 Developer Notes`.

## TASK-MOS-006.P3 Done when

- **TASK-MOS-006.DW1:** Root specs describe the implemented default repository-canonical selection, mill-owned launch source, reduced config JSON, and explicit `--config-dir` isolation behavior.
- **TASK-MOS-006.DW2:** Relevant validation commands have passed or failures are documented with a clear follow-up blocker.
- **TASK-MOS-006.DW3:** `git status --short` shows no generated SQLite, runtime metadata, or disposable worktree artifacts.

## TASK-MOS-006.P4 Out of scope

- **TASK-MOS-006.OS1:** Archiving the feature folder; devflow finish/archive owns that after implementation is accepted.
- **TASK-MOS-006.OS2:** Adding new product behavior beyond the accepted proposal/spec deltas.

## TASK-MOS-006.P5 References

- **TASK-MOS-006.REF1:** [specs/cli.delta.md](../specs/cli.delta.md).
- **TASK-MOS-006.REF2:** [specs/daemon-runtime.delta.md](../specs/daemon-runtime.delta.md).
- **TASK-MOS-006.REF3:** [mill-owned-source.plan.md](../mill-owned-source.plan.md) `PLAN-MOS-001.PH4` and `PLAN-MOS-001.V1`-`V4`.
