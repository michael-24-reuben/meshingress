package dev.mrk.meshingress.mcp;

import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.function.McpFunctionDescriptor;
import dev.mrk.meshingress.dispatch.GeneratedJsonContent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputSchemaValidatorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OutputSchemaValidator validator = new OutputSchemaValidator(objectMapper);

    @Test
    void appendsWarningMetadataWithoutChangingAnInvalidStructuredResult() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("count", "not-a-number");
        DispatchExecutionResult result = DispatchExecutionResult.builder()
                .structuredContent(new GeneratedJsonContent(payload))
                .build();

        validator.validate(result, functionWith(schemaRequiringNumericCount()));

        ObjectNode serialized = result.toJson(objectMapper);
        assertFalse(result.isError());
        assertEquals("not-a-number", serialized.at("/structuredContent/data/count").asString());
        assertTrue(serialized.at("/_meta/meshingress/outputSchema/validated").asBoolean());
        assertFalse(serialized.at("/_meta/meshingress/outputSchema/valid").asBoolean());
        assertEquals("warn", serialized.at("/_meta/meshingress/outputSchema/policy").asString());
        assertEquals("type", serialized.at("/_meta/meshingress/outputSchema/violations/0/keyword").asString());
    }

    @Test
    void marksMatchingTypedEnvelopeAsValid() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("count", 3);
        DispatchExecutionResult result = DispatchExecutionResult.builder()
                .structuredContent(new GeneratedJsonContent(payload))
                .build();

        validator.validate(result, functionWith(schemaRequiringNumericCount()));

        ObjectNode serialized = result.toJson(objectMapper);
        assertTrue(serialized.at("/_meta/meshingress/outputSchema/validated").asBoolean());
        assertTrue(serialized.at("/_meta/meshingress/outputSchema/valid").asBoolean());
        assertFalse(serialized.at("/_meta/meshingress/outputSchema/violations").isArray());
    }

    private McpFunctionDescriptor functionWith(ObjectNode outputSchema) {
        return new McpFunctionDescriptor(
                "test.tool.function",
                "Test Function",
                "",
                1,
                true,
                ToolVisibility.PUBLIC,
                "test.handler",
                objectMapper.createObjectNode(),
                outputSchema,
                objectMapper.createObjectNode(),
                false
        );
    }

    private ObjectNode schemaRequiringNumericCount() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("kind").put("type", "string");
        properties.putObject("schema").put("type", "string");
        properties.putObject("version").put("type", "integer");
        ObjectNode data = properties.putObject("data");
        data.put("type", "object");
        ObjectNode dataProperties = data.putObject("properties");
        dataProperties.putObject("count").put("type", "number");
        data.putArray("required").add("count");
        schema.putArray("required").add("kind").add("schema").add("version").add("data");
        return schema;
    }
}
