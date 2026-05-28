# Context

## Source Report

- `architect/reports/redundant-code-inspection-2026-05-28-171500.md`

## Relevant Files

- `app/meshingress-server/src/main/java/dev/mrk/meshingress/mcp/tools/registry/InMemoryToolRegistry.java`

## Notes

- The registry repeats visibility and allow/deny checks for tools and functions.
- Consolidation should not change public or role-gated visibility behavior.

