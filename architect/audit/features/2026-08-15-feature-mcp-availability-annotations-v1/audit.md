# MCP availability annotations

**Feature ID:** `feature-mcp-availability-annotations-v1`  
**Recorded:** 2026-08-15T19:45:33-04:00  
**Status:** Implemented

## Feature memo

Tool annotations can describe static availability in the MCP tool descriptor:

```json
{
  "availability": {
    "version": 1,
    "mode": "all",
    "conditions": [
      { "type": "dev.mrk.availability.day-of-week", "parameters": { "days": ["MONDAY"] } }
    ]
  }
}
```

The scanner emits this only when a function has a recognized availability annotation. Version 1 supports day-of-week, feature-flag, and time-range condition descriptors. Studio models the node rather than treating it as unstructured data, while preserving room for other annotation fields.

## Boundary

The policy evaluation shown here happens during annotation scanning with `SCAN_AVAILABILITY`; it is not a per-`tools/call` argument-aware authorization gate. Dynamic request-argument availability and runtime enforcement require a separate execution-time design.

## Evidence snapshot

- `McpToolAnnotationScanner.java` serializes the stable descriptor and maps supported annotation types.
- The same scanner evaluates registered policies in scan mode.
- `mcp.ts` defines `McpAvailability` and attaches it to function annotations.

## Origin and currency

This is an implementation snapshot from source inspected on 2026-08-15. It is not a source of truth; verify the cited files before relying on the exact schema or runtime boundary.
