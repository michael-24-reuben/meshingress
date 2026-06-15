package dev.mrk.meshingress.controller.roles.params;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "Patch object for roles/tools/update.")
public record ToolPatchParams(
        @Schema(description = "Replacement human-readable title.", example = "List architect entries")
        String title,
        @Schema(description = "Replacement human-readable description.", example = "List architect entries through a dynamic alias.")
        String description,
        @Schema(description = "Replacement enabled flag.", example = "true")
        Boolean enabled,
        @Schema(description = "Replacement registry visibility.", allowableValues = {"public", "private", "admin"}, example = "public")
        String visibility,
        @Schema(description = "Replacement handler key.", example = "architect.entries.list")
        String handlerKey,
        @Schema(description = "Replacement JSON Schema object describing accepted input arguments.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode inputSchema,
        @Schema(description = "Replacement JSON Schema object describing structured output.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode outputSchema,
        @Schema(description = "Replacement MCP annotations object.", type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        JsonNode annotations
) {
}

