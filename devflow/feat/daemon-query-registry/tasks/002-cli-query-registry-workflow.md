# CLI Named Query Consumption

**Document ID:** `TASK-002`

## TASK-002.P1 Scope

Type: AFK

Keep the CLI as a small consumer of daemon runtime state: remove CLI query-file loading and make `list --query name` / `ready --query name` resolve against daemon memory.

## TASK-002.P2 References

- **TASK-002.R1:** [Feature plan](../daemon-query-registry.plan.md)
- **TASK-002.R2:** [CLI delta](../specs/cli.delta.md)
- **TASK-002.R3:** `src/todo/cli.clj`, `test/todo/cli_test.clj`

## TASK-002.P3 Implementation notes

- **TASK-002.I1:** Do not add a CLI `query` command group.
- **TASK-002.I2:** Remove `--query-file` from CLI parsing, usage, and specs/tests.
- **TASK-002.I3:** Keep `--where` and `--query` mutually exclusive for `list` and `ready`.
- **TASK-002.I4:** Route `list --query name` and `ready --query name` through daemon-memory query resolution.
- **TASK-002.I5:** Keep `--param key=value` for runtime parameters to daemon-memory named queries.
- **TASK-002.I6:** Missing named queries must fail non-zero with a clear message. Include available names if doing so is simple.

## TASK-002.P4 Done when

- **TASK-002.D1:** CLI `list --query name` and `ready --query name` use a query registered in daemon memory.
- **TASK-002.D2:** CLI `--query-file` is no longer accepted or documented.
- **TASK-002.D3:** CLI has no query registry mutation or listing commands.
- **TASK-002.D4:** `--query missing-name` fails clearly and non-zero.
- **TASK-002.D5:** Relevant CLI tests pass.
