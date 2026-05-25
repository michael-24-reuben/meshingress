# MCP Tool Availability Annotations

## Objective

Design and implement MCP tool availability annotations in the tool annotation and tool framework modules.

Tool availability belongs to MCP tool metadata and execution policy, not to HTTP route metadata.

## Target Surface

Tool availability should live under tool-owned modules:

```txt
lib/meshingress-tool-annotations
lib/meshingress-tool-framework
```

The route modules should not define or execute tool availability.

## Expected Shape

Tool modules should be able to declare availability constraints near the tool or tool function metadata.

Examples:

```java
@EnableWhenFeatureFlagOn("instagram.publish.enabled")
@EnableOnDays({MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY})
@EnableWithinTimeRanges(zone = "America/New_York", ranges = {"09:00-17:00"})
@McpTool(...)
public class InstagramPublishTool {
    ...
}
```

## Scope Boundary

This architect entry covers tool-level and tool-function-level availability metadata, validation, and runtime evaluation.

It should not change HTTP server route configuration. Actual server paths remain owned by the route annotation framework.
