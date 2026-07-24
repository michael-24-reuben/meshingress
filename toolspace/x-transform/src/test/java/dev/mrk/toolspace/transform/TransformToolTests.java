package dev.mrk.toolspace.transform;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TransformToolTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listTypesExposesTheEnumWithoutCreatingOneToolPerTransform() {
        TransformTool tool = tool();

        var result = tool.listTypes(null);

        assertThat(result.isError()).as(result.toJson(objectMapper).toString()).isFalse();
        assertThat(result.structuredContent().orElseThrow().path("kind").asString()).isEqualTo("data.records");
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().value().size()).isEqualTo(TransformType.values().length);
    }

    @Test
    void transformReturnsTypedCodeStructuredContent() {
        TransformTool tool = tool();

        var result = tool.transform(new TransformArgs(TransformType.JSON_TO_JAVA, "{\"name\":\"Ada\"}", null, null), null);

        assertThat(result.isError()).as(result.toJson(objectMapper).toString()).isFalse();
        assertThat(result.structuredContent().orElseThrow().path("kind").asString()).isEqualTo("text.code");
        assertThat(result.structuredContent().orElseThrow().path("data").path("language").asString()).isEqualTo("java");
        assertThat(result.content().getFirst().value().asString()).contains("public class Root");
    }

    private TransformTool tool() {
        TransformProperties properties = new TransformProperties("node", "src/main/resources/transform-backend", 120_000L);
        return new TransformTool(objectMapper, new TransformBackend(objectMapper, properties));
    }
}
