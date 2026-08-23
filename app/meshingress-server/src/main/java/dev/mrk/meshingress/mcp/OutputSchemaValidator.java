package dev.mrk.meshingress.mcp;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.PathType;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;

/**
 * Applies the declared MCP output schema as an observational, warn-only policy.
 * <p>
 * Validation deliberately happens after a tool returns, so it never changes the
 * result payload or turns a successful call into an error. The bounded result is
 * added to {@code _meta.meshingress.outputSchema} for MCP clients.
 */
@Component
public final class OutputSchemaValidator {

    private static final int MAX_VIOLATIONS = 20;
    private static final int MAX_DIAGNOSTIC_TEXT_LENGTH = 512;

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;

    public OutputSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        SchemaRegistryConfig configuration = SchemaRegistryConfig.builder()
                .pathType(PathType.JSON_POINTER)
                .failFast(false)
                .build();
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12,
                builder -> builder
                        .schemaRegistryConfig(configuration)
                        .schemaLoader(loader -> loader.fetchRemoteResources(false))
        );
    }

    /**
     * Validates the structured-content envelope when a function declares an output schema.
     * Calls without either declaration or structured content remain unannotated.
     */
    public void validate(DispatchExecutionResult result, McpFunctionDescriptor function) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(function, "function must not be null");

        JsonNode outputSchema = function.outputSchema();
        JsonNode structuredContent = result.toJson(objectMapper).get("structuredContent");
        if (outputSchema == null || !outputSchema.isObject() || structuredContent == null || structuredContent.isNull()) {
            return;
        }

        ObjectNode validation = validationMetadata(result);
        validation.put("policy", "warn");
        try {
            Schema schema = schemaRegistry.getSchema(outputSchema);
            List<Error> errors = schema.validate(structuredContent);
            validation.put("validated", true);
            validation.put("valid", errors.isEmpty());
            if (!errors.isEmpty()) {
                ArrayNode violations = validation.putArray("violations");
                errors.stream().limit(MAX_VIOLATIONS).forEach(error -> violations.add(violation(error)));
                if (errors.size() > MAX_VIOLATIONS) {
                    validation.put("truncated", true);
                    validation.put("totalViolations", errors.size());
                }
            }
        } catch (RuntimeException exception) {
            // Dynamic descriptors can contain an invalid or unsupported schema. Do not make a tool call fail.
            validation.put("validated", false);
            validation.put("reason", "The declared output schema could not be evaluated.");
        }
    }

    private ObjectNode validationMetadata(DispatchExecutionResult result) {
        JsonNode currentMeta = result.toJson(objectMapper).path("_meta");
        ObjectNode meta = currentMeta.isObject()
                ? ((ObjectNode) currentMeta).deepCopy()
                : objectMapper.createObjectNode();
        JsonNode currentMeshingress = meta.get("meshingress");
        ObjectNode meshingress = currentMeshingress != null && currentMeshingress.isObject()
                ? ((ObjectNode) currentMeshingress).deepCopy()
                : objectMapper.createObjectNode();
        ObjectNode outputSchema = objectMapper.createObjectNode();
        meshingress.set("outputSchema", outputSchema);
        meta.set("meshingress", meshingress);
        result.setMeta(meta);
        return outputSchema;
    }

    private ObjectNode violation(Error error) {
        ObjectNode violation = objectMapper.createObjectNode();
        violation.put("instanceLocation", truncate(error.getInstanceLocation().toString()));
        violation.put("schemaLocation", truncate(error.getSchemaLocation().toString()));
        violation.put("keyword", truncate(error.getKeyword()));
        violation.put("message", truncate(error.getMessage()));
        return violation;
    }


    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= MAX_DIAGNOSTIC_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DIAGNOSTIC_TEXT_LENGTH - 3) + "...";
    }
}
