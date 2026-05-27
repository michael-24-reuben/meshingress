package dev.mrk.meshingress.framework.scanning;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpFunctionParam;
import dev.mrk.meshingress.api.tools.annotation.McpInputField;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolAnnotationScannerTests {

    private final McpToolAnnotationScanner scanner = new McpToolAnnotationScanner(new ObjectMapper());

    @Test
    void scansToolMetadataAndDefaultFunctionSchema() {
        AnnotatedMcpTool tool = scanner.scan(HelloWorldAnnotatedTool.class);

        assertEquals("tools", tool.mapping());
        assertEquals("helloworld", tool.descriptor().name());
        assertEquals("Hello World", tool.descriptor().title());
        assertEquals(ToolVisibility.PUBLIC, tool.descriptor().visibility());
        assertEquals("LOCAL_READ", tool.descriptor().annotations().path("scopes").get(0).asString());
        assertEquals("EXTERNAL_API_READ", tool.functions().getFirst().annotations().path("scopes").get(0).asString());

        assertEquals(1, tool.functions().size());
        assertEquals("call", tool.functions().getFirst().name());
        assertEquals("helloworld.call", tool.functions().getFirst().descriptor().name());
        assertEquals("helloworld.call", tool.functions().getFirst().descriptor().handlerKey());
        assertEquals("tools/call", tool.functions().getFirst().path());
        assertEquals("Invoke the tool with the provided arguments.", tool.functions().getFirst().description());
        assertEquals("object", tool.functions().getFirst().inputSchema().path("type").asString());
        assertEquals(
                "The name of the person to greet.",
                tool.functions().getFirst().inputSchema().path("properties").path("name").path("description").asString()
        );
        assertEquals("name", tool.functions().getFirst().inputSchema().path("required").get(0).asString());
    }

    @Test
    void normalizesMappingAndFunctionSegments() {
        AnnotatedMcpTool tool = scanner.scan(SlashedAnnotatedTool.class);

        assertEquals("instagram", tool.mapping());
        assertEquals("instagram/fetch", tool.functions().getFirst().path());
    }

    @Test
    void infersSingleArgsObjectParameterWithoutFunctionParamAnnotation() {
        AnnotatedMcpTool tool = scanner.scan(InferredArgsAnnotatedTool.class);

        assertEquals(1, tool.functions().getFirst().parameters().size());
        assertEquals("args", tool.functions().getFirst().parameters().getFirst().name());
        assertEquals(HelloWorldGreetArgs.class, tool.functions().getFirst().parameters().getFirst().bindType());
        assertEquals("name", tool.functions().getFirst().descriptor().inputSchema().path("required").get(0).asString());
    }

    @McpTool(
            value = "helloworld",
            title = "Hello World",
            description = "Return a greeting from an external Meshingress tool module."
    )
    @McpToolScopes(McpToolScope.LOCAL_READ)
    @McpToolMapping("tools")
    static class HelloWorldAnnotatedTool {

        @McpFunction(
                value = "call",
                description = "Invoke the tool with the provided arguments.",
                visibility = ToolVisibility.PUBLIC
        )
        @McpToolScopes(McpToolScope.EXTERNAL_API_READ)
        ObjectNode call(
                @McpFunctionParam(value = "args", description = "Greet a person by name.")
                HelloWorldGreetArgs arguments,
                McpCallContext context
        ) {
            return null;
        }
    }

    record HelloWorldGreetArgs(
            @McpInputField(description = "The name of the person to greet.")
            String name
    ) {
    }

    @McpTool(value = "instagram.fetch")
    @McpToolMapping("/instagram/")
    static class SlashedAnnotatedTool {

        @McpFunction("/fetch")
        ObjectNode fetch(@McpFunctionParam("args") ObjectNode args) {
            return args;
        }
    }

    @McpTool(value = "helloworld.inferred")
    static class InferredArgsAnnotatedTool {

        @McpFunction("call")
        ObjectNode call(HelloWorldGreetArgs arguments, McpCallContext context) {
            return null;
        }
    }
}
