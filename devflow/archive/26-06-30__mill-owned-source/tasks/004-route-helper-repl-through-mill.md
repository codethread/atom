# Task 4: Route helper repl through mill

**Document ID:** `TASK-MOS-004`

## TASK-MOS-004.P1 Scope

Type: AFK

Stop `strand weaver repl` from reading source out of config and route its launch context through mill-owned selected-world/source resolution.

## TASK-MOS-004.P2 Must implement exactly

- **TASK-MOS-004.MI1:** Add or reuse a mill operation that returns the selected world's running status plus resolved launch source for helper REPL startup without exposing source as selected-world identity.
- **TASK-MOS-004.MI2:** Update `cli/internal/command.launchRepl` so it does not call `config.Load(...).Source` or `config.ResolveSource` locally from persisted config.
- **TASK-MOS-004.MI3:** Preserve existing REPL behavior: fail if selected-world weaver is not running, pass selected config/state metadata to `skein.repl`, support `--stdin`, and print raw Clojure results rather than JSON envelopes.
- **TASK-MOS-004.MI4:** Ensure normal public `weaver status` JSON does not require source to be part of user-facing identity; if source is returned for the private REPL operation, keep it operation-specific.
- **TASK-MOS-004.MI5:** Add/update command and mill client tests for REPL launch using mill-returned source and no `config.json` source.

## TASK-MOS-004.P3 Done when

- **TASK-MOS-004.DW1:** `strand weaver repl` and `strand weaver repl --stdin` launch from mill-resolved source with reduced config JSON.
- **TASK-MOS-004.DW2:** A stopped selected-world weaver still produces the existing fail-loud remediation to run `strand weaver start`.
- **TASK-MOS-004.DW3:** `(cd cli && go test ./internal/command ./internal/client ./cmd/mill)` passes for touched areas.

## TASK-MOS-004.P4 Out of scope

- **TASK-MOS-004.OS1:** Adding arbitrary public source-inspection commands.
- **TASK-MOS-004.OS2:** Changing Clojure REPL helper APIs beyond launch context plumbing.

## TASK-MOS-004.P5 References

- **TASK-MOS-004.REF1:** [specs/cli.delta.md](../specs/cli.delta.md) `DELTA-MOS-CLI-001.CC6` and `CC7`.
- **TASK-MOS-004.REF2:** [mill-owned-source.plan.md](../mill-owned-source.plan.md) `PLAN-MOS-001.A5`.
- **TASK-MOS-004.REF3:** `cli/internal/command/command.go`, `cli/internal/client/mill.go`, `cli/cmd/mill/main.go`.
