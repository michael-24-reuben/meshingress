package dev.mrk.meshingress.api.result;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispatchExecutionResultTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedStructuredContentUsesTheCanonicalEnvelopeAndOriginalData() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("exitCode", 0);
        payload.put("stdout", "done");

        ObjectNode result = DispatchExecutionResult.builder()
                .structuredContent(payload)
                .build()
                .toJson(objectMapper);
        ObjectNode structuredContent = (ObjectNode) result.path("structuredContent");

        assertEquals("generated.json.object", structuredContent.path("kind").asString());
        assertEquals("meshingress.generated.json.object.v1", structuredContent.path("schema").asString());
        assertEquals(1, structuredContent.path("version").asInt());
        assertEquals(payload, structuredContent.path("data"));
        assertEquals(0, structuredContent.path("data").path("data").size());
    }

    @Test
    void rawStructuredContentStillPreservesTheCallerProvidedWireShape() {
        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("legacy", true);

        ObjectNode result = DispatchExecutionResult.builder()
                .structuredContent(raw)
                .build()
                .toJson(objectMapper);

        assertEquals(raw, result.path("structuredContent"));
    }
}
