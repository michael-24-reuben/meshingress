# Context

## Background

Availability annotations were initially explored inside the route annotation framework. That created a confusing ownership boundary because route annotations describe HTTP server paths, while the desired availability behavior applies to MCP tools and tool functions.

The route framework has now been narrowed back to route metadata, middleware, route registry validation, and server path attachment.

## Current Code Areas

Tool-owned annotation and framework code currently appears under:

```txt
lib/meshingress-tool-annotations/src/main/java/dev/mrk/meshingress/api/tools/annotation
lib/meshingress-tool-framework/src/main/java/dev/mrk/meshingress/tools/availability
```

The exact package layout should be reviewed before implementation so annotations, validation conditions, and runtime policy implementations are not duplicated across modules.

## Design Constraint

Do not reintroduce tool availability into:

```txt
lib/meshingress-route-api
lib/meshingress-route-annotations
lib/meshingress-route-framework
```

If route-level scheduling is ever needed, it should be a separate decision with a separate name and contract.

