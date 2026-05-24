package dev.mrk.meshingress.route.api;

public record McpConfigureRouteMapping(
        boolean audit,
        boolean debugTrace,
        Long timeoutMs,
        McpRouteStability stability
) {
    // This is a compact constructor - notice no parameters!
    public McpConfigureRouteMapping {
        // 1. Primitive longs can't be null, so we just check the value
        if (timeoutMs != null && timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs must be >= 0");
        }

        // 2. We can reassign the incoming parameter variable directly here
        if (stability == null) {
            stability = McpRouteStability.STABLE;
        }

        // The compiler automatically assigns the fields at the end of this block!
    }
}