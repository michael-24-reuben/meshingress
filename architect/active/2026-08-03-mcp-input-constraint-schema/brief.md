# MCP Input Constraint Schema

## Goal

Allow tool authors to declare type-specific input constraints alongside `@McpInputField`, compile those constraints into MCP JSON Schema, and keep the existing Studio parameter-type selection centralized and schema-driven.

## Scope

- Companion constraint annotations such as `@McpIntegerConstraints` and `@McpNumberConstraints`.
- A shared compiled constraint model, including image constraints extending file constraints at runtime.
- JSON Schema compilation for numeric, array, file, and image constraints.
- Generic collection item-schema discovery.
- Toonverse search bounds and defaults.
- A Studio-only refactor that centralizes its existing `type`/`format`/`enum` parameter-type reads.
- Synchronize compiled constraint metadata with Studio parameter controls and presentation.

## Explicit Non-goals

- Do not add a backend `objectType` discriminator for Studio.
- Do not silently apply fallback values to invalid requests.
- Do not replace the existing active root assignment or handoff, which belongs to unrelated Nextcloud work.
