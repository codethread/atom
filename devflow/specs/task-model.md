# Task Model

**Document ID:** `SPEC-001`
**Status:** Implemented
**Last Updated:** 2026-06-24
**Code:** `src/todo/db.clj`

## SPEC-001.P1 Purpose

The task model defines the durable local data contract for the todo graph: task records, first-class lifecycle state, open-ended JSON attributes, typed task-to-task edges, and readiness semantics.

## SPEC-001.P2 Task records

A task has:

- `id` — generated unique text id.
- `title` — non-blank task title.
- `status` — one of `todo`, `done`, `failed`, or `cancelled`.
- `attributes` — userland JSON object stored as SQLite `TEXT`.
- `created_at` — set on insert.
- `updated_at` — changed on task update.
- `final_at` — set when status reaches a final state; null for `todo`.

Final statuses are `done`, `failed`, and `cancelled`.

## SPEC-001.P3 Attributes

Attributes are userland task fields such as priority, owner, estimates, due dates, and external references. They are not task lifecycle metadata. Attribute values must encode to a JSON object; omitted or nil attributes normalize to `{}`.

## SPEC-001.P4 Edges

Task edges connect `from_task_id` to `to_task_id`, have an `edge_type`, and have JSON object attributes. Allowed edge types are `depends-on`, `related-to`, `parent-of`, and `supersedes`.

A `depends-on` edge from task `A` to task `B` means `A` is blocked by `B` until `B` reaches a final status.

The edge graph is acyclic. Self-edges and writes that introduce a directed cycle fail.

## SPEC-001.P5 Readiness

A ready task is a non-final task with no direct `depends-on` dependency whose task is still non-final.

## SPEC-001.P6 Persistence

The `tasks` table stores lifecycle fields as columns and attributes as JSON `TEXT`. The `task_edges` table stores typed relationships and edge attributes as JSON `TEXT`. JSONB assumptions are not part of this contract.

## SPEC-001.P7 Deferred

The future query DSL for filtering `list` and `ready` is tracked by [RFC-002](../rfcs/2026-06-24-task-query-dsl.md). Attribute-level metadata and per-attribute timestamps are not part of the current model.
