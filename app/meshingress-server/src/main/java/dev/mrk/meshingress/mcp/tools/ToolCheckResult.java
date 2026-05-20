package dev.mrk.meshingress.mcp.tools;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record ToolCheckResult(
        boolean valid,
        List<String> errors,
        List<String> warnings,
        JsonNode normalized
) {
}
