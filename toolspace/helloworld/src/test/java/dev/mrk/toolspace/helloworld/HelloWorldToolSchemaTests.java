package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloWorldToolSchemaTests {

    @Test
    void exposesEveryStudioObjectTypeWhileOnlyRequiringName() {
        AnnotatedMcpTool tool = new McpToolAnnotationScanner(new ObjectMapper(), new HelloWorldManifest()).scan(HelloWorldTool.class);
        assertEquals("helloworld.greeting", tool.descriptor().name());
        assertEquals("helloworld.greeting.greet", tool.functions().getFirst().descriptor().name());
        ObjectNode schema = tool.functions().getFirst().inputSchema();
        ObjectNode properties = schema.withObject("properties");

        assertEquals(25, properties.size());
        assertEquals(1, schema.path("required").size());
        assertEquals("name", schema.path("required").get(0).asString());
        assertEquals(1, properties.path("name").path("minLength").asInt());
        assertEquals(120, properties.path("name").path("maxLength").asInt());

        assertType(properties, "text", "string");
        assertEquals(1, properties.path("text").path("minLength").asInt());
        assertEquals(160, properties.path("text").path("maxLength").asInt());
        assertType(properties, "number", "integer");
        assertEquals(1, properties.path("number").path("minimum").asInt());
        assertEquals(5, properties.path("number").path("maximum").asInt());
        assertEquals("slider", properties.path("number").path("x-mcp-control").asString());
        assertFormat(properties, "json", "json");
        assertEquals(12, properties.path("json").path("maxProperties").asInt());
        assertType(properties, "array", "array");
        assertEquals(8, properties.path("array").path("maxItems").asInt());
        assertEquals(true, properties.path("array").path("uniqueItems").asBoolean());
        assertType(properties, "booleanValue", "boolean");
        assertEquals(false, properties.path("booleanValue").path("default").asBoolean());
        assertFormat(properties, "date", "date");
        assertEquals("2100-12-31", properties.path("date").path("x-mcp-formatMaximum").asString());
        assertFormat(properties, "binary", "binary");
        assertEquals(4096, properties.path("binary").path("x-mcp-maxBytes").asInt());
        assertType(properties, "nullValue", "null");
        assertFormat(properties, "url", "uri");
        assertEquals("https", properties.path("url").path("x-mcp-allowedSchemes").get(0).asString());
        assertFormat(properties, "file", "file");
        assertEquals(10_485_760, properties.path("file").path("x-mcp-maxBytes").asInt());
        assertFormat(properties, "unknown", "unknown");
        assertEquals("object", properties.path("unknown").path("x-mcp-allowedSchemaTypes").get(4).asString());
        assertFormat(properties, "secret", "password");
        assertEquals(8, properties.path("secret").path("minLength").asInt());
        assertEquals(4, properties.path("enumValue").path("enum").size());
        assertEquals("sunny", properties.path("enumValue").path("default").asString());
        assertFormat(properties, "code", "code");
        assertEquals("java", properties.path("code").path("x-mcp-language").asString());
        assertFormat(properties, "email", "email");
        assertEquals(254, properties.path("email").path("maxLength").asInt());
        assertType(properties, "decimal", "number");
        assertEquals(0.1, properties.path("decimal").path("multipleOf").asDouble());
        assertEquals("slider", properties.path("decimal").path("x-mcp-control").asString());
        assertFormat(properties, "image", "image");
        assertEquals(5_242_880, properties.path("image").path("x-mcp-maxBytes").asInt());
        assertFormat(properties, "clock", "clock");
        assertEquals("23:59", properties.path("clock").path("x-mcp-formatMaximum").asString());
        assertFormat(properties, "time", "time");
        assertEquals("23:59", properties.path("time").path("x-mcp-formatMaximum").asString());
        assertFormat(properties, "duration", "duration");
        assertEquals("PT24H", properties.path("duration").path("x-mcp-formatMaximum").asString());
        assertFormat(properties, "interval", "interval");
        assertEquals("PT24H", properties.path("interval").path("x-mcp-maxDuration").asString());
        assertFormat(properties, "color", "color");
        assertEquals("hex", properties.path("color").path("x-mcp-allowedColorFormats").get(0).asString());
        assertFormat(properties, "location", "location");
        assertFormat(properties, "geo", "geo");
        assertEquals(-90.0, properties.path("geo").path("x-mcp-minLatitude").asDouble());
    }

    private void assertType(ObjectNode properties, String name, String type) {
        assertEquals(type, properties.path(name).path("type").asString());
    }

    private void assertFormat(ObjectNode properties, String name, String format) {
        assertEquals(format, properties.path(name).path("format").asString());
    }
}
