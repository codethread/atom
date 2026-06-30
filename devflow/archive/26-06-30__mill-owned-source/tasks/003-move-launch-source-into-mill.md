# Task 3: Move launch source into mill

**Document ID:** `TASK-MOS-003`

## TASK-MOS-003.P1 Scope

Type: AFK

Make mill the source-resolution authority for starting weavers, without reading source from selected config JSON.

## TASK-MOS-003.P2 Must implement exactly

- **TASK-MOS-003.MI1:** Add mill-side source resolution for weaver launch that prefers `SKEIN_SOURCE`, then build-time `InstalledSource`, then the repository-canonical root from the request cwd when it is a Skein checkout containing `deps.edn`.
- **TASK-MOS-003.MI2:** Keep the existing source validation invariants: resolved source must be absolute, existing, and contain `deps.edn`; invalid or absent source fails loudly with actionable remediation.
- **TASK-MOS-003.MI3:** Update `cli/cmd/mill/lifecycle.go` so `startWeaver` resolves source via mill-owned launch context instead of `config.Load(...).Source`.
- **TASK-MOS-003.MI4:** Ensure the launched Clojure process still uses resolved source as `cmd.Dir` and receives explicit `--config-dir`, `--state-dir`, and `--data-dir` args.
- **TASK-MOS-003.MI5:** Add/update mill lifecycle tests for source precedence and no-source failure without relying on `config.json` source.

## TASK-MOS-003.P3 Done when

- **TASK-MOS-003.DW1:** `strand weaver start` can start from a selected world whose `config.json` has no source field when mill can resolve source through env, installed source, or a Skein checkout cwd.
- **TASK-MOS-003.DW2:** Missing/unresolvable source fails before launch with a message that points to `SKEIN_SOURCE`, install-time source, or Skein checkout cwd remediation.
- **TASK-MOS-003.DW3:** `(cd cli && go test ./cmd/mill ./internal/config)` passes for touched areas.

## TASK-MOS-003.P4 Out of scope

- **TASK-MOS-003.OS1:** Helper REPL launch context; task 4 owns that.
- **TASK-MOS-003.OS2:** Changing weaver Clojure runtime startup-file behavior.

## TASK-MOS-003.P5 References

- **TASK-MOS-003.REF1:** [specs/daemon-runtime.delta.md](../specs/daemon-runtime.delta.md) `DELTA-MOS-RUNTIME-001.CC4`.
- **TASK-MOS-003.REF2:** [mill-owned-source.plan.md](../mill-owned-source.plan.md) `PLAN-MOS-001.A4` and `PLAN-MOS-001.PH2`.
- **TASK-MOS-003.REF3:** `cli/cmd/mill/lifecycle.go`, `cli/cmd/mill/main.go`, `cli/internal/config/bootstrap.go`.
