package dev.mrk.toolspace.transform;

import dev.mrk.meshingress.api.tools.annotation.McpInputField;
import tools.jackson.databind.JsonNode;

public record TransformArgs(
        @McpInputField(value = "type", description = "A supported Transform conversion identifier.", required = true) TransformType type,
        @McpInputField(value = "value", description = "Primary source text to transform.", required = true) String value,
        @McpInputField(value = "secondaryValue", description = "Optional GraphQL document or JSON-LD context/frame.", required = false) String secondaryValue,
        @McpInputField(value = "settings", description = "Optional transform-specific settings object.", required = false) JsonNode settings
) { }
