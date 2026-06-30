# Task 2: Remove persisted source config

**Document ID:** `TASK-MOS-002`

## TASK-MOS-002.P1 Scope

Type: AFK

Reduce selected config workspace JSON to the alpha marker contract and stop `strand init` from requiring or writing a persisted Skein source checkout path.

## TASK-MOS-002.P2 Must implement exactly

- **TASK-MOS-002.MI1:** Update `cli/internal/config.Config`, `Load`, and allowed-key validation so `config.json` supports `configFormat` only for the new alpha contract.
- **TASK-MOS-002.MI2:** Update bootstrap so `BootstrapWorld` writes `{"configFormat":"alpha"}` when creating `config.json` and does not call source resolution as a prerequisite for init.
- **TASK-MOS-002.MI3:** Preserve creation/completion of `libs/`, `libs.edn`, `init.clj`, and `.gitignore`; do not overwrite existing user files.
- **TASK-MOS-002.MI4:** Remove public `strand init --source` support and update CLI help/error text so init is described only as selected config workspace bootstrap. Source is resolved later by mill from `SKEIN_SOURCE`, installed build source, or canonical Skein checkout cwd.
- **TASK-MOS-002.MI5:** Remove or ignore the mill request `Source` field for public init flow, unless a later task needs an internal-only field for tests; do not expose source as config or selected-world state.
- **TASK-MOS-002.MI6:** Update Go tests that asserted `source` in config to assert the reduced marker schema and fail-loud unsupported keys.

## TASK-MOS-002.P3 Done when

- **TASK-MOS-002.DW1:** New `config.json` files contain `configFormat` and no `source` field.
- **TASK-MOS-002.DW2:** Loading a config containing a `source` key fails loudly as an unsupported key.
- **TASK-MOS-002.DW3:** `(cd cli && go test ./internal/config ./internal/command)` passes for touched areas.

## TASK-MOS-002.P4 Out of scope

- **TASK-MOS-002.OS1:** Changing mill weaver start source resolution; task 3 owns that.
- **TASK-MOS-002.OS2:** Supporting legacy `source` compatibility as a long-term contract.

## TASK-MOS-002.P5 References

- **TASK-MOS-002.REF1:** [specs/cli.delta.md](../specs/cli.delta.md) `DELTA-MOS-CLI-001.CC3` and `CC4`.
- **TASK-MOS-002.REF2:** [mill-owned-source.plan.md](../mill-owned-source.plan.md) `PLAN-MOS-001.A3` and `PLAN-MOS-001.CM1`.
- **TASK-MOS-002.REF3:** `cli/internal/config/config.go`, `cli/internal/config/bootstrap.go`, `cli/internal/command/command.go`.
