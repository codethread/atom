# Daemon Query Registry API

**Document ID:** `TASK-001`

## TASK-001.P1 Scope

Type: AFK

Add daemon-owned in-memory query registry support and client wrappers. The registry must live for the daemon lifetime and be shared by all CLI/REPL clients connected to that daemon.

## TASK-001.P2 References

- **TASK-001.R1:** [Feature plan](../daemon-query-registry.plan.md)
- **TASK-001.R2:** [Daemon runtime delta](../specs/daemon-runtime.delta.md)
- **TASK-001.R3:** `src/todo/daemon/runtime.clj`, `src/todo/daemon/api.clj`, `src/todo/client.clj`, `src/todo/query.clj`

## TASK-001.P3 Implementation notes

- **TASK-001.I1:** Extend daemon runtime state with an atom or equivalent holding query-name to query-definition mappings.
- **TASK-001.I2:** Add daemon API operations to register one query, load many queries, return the registry, and resolve missing names with a clear error.
- **TASK-001.I3:** Accept simple symbol or keyword query names and canonicalize lookup/storage so `mine` and `:mine` refer to one registry entry; reject namespaced or non-simple names clearly.
- **TASK-001.I4:** Validate query definitions on registration/load using existing query validation, adjusting validation if needed so valid parameterized queries are accepted.
- **TASK-001.I5:** Add client wrappers for the new API operations.
- **TASK-001.I6:** Keep query definitions as EDN data arguments to fixed daemon API forms.

## TASK-001.P4 Done when

- **TASK-001.D1:** Daemon registry contents are shared across separate client calls for the same running daemon.
- **TASK-001.D2:** Missing query names throw/return a clear domain error including the requested name.
- **TASK-001.D3:** Symbol and keyword forms of one simple name resolve to the same entry.
- **TASK-001.D4:** Existing daemon/client tests pass, with new focused tests for registry add/load/list/resolve.
