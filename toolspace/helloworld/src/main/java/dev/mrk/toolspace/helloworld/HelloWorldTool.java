package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.tools.ToolExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.tools.availability.featureflag.EnableWhenFeatureFlagOn;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@McpTool(
        value = "helloworld.greet",
        title = "Hello World",
        description = "Return a greeting from an external Meshingress tool module."
)
@McpToolScopes(McpToolScope.USER_WRITE)
@McpToolMapping("tools")
public class HelloWorldTool {

    private final ObjectMapper objectMapper;

    public HelloWorldTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @McpConfigureMapping(
            secrets = @McpSecret(name = "instagramApiToken", ref = "instagram-api-token"),
            availabilityMode = McpAvailabilityMode.ALL,
            audit = true,
            debugTrace = true,
            timeoutMs = 20_000
    )
    @EnableWhenFeatureFlagOn("instagram.publish.enabled")
    @McpFunction(value = "call", description = "Greet the Person.")
    public ToolExecutionResult call(HelloWorldGreetArgs arguments, McpCallContext context) {
        String name = arguments.getName();

        ObjectNode structured = objectMapper.createObjectNode();
        structured.put("message", "Hello, " + name + "!");

        ArrayNode content = objectMapper.createArrayNode();

        ObjectNode text = objectMapper.createObjectNode();
        text.put("type", "text");
        text.put("text", "Hello, " + name + "!");
        content.add(text);

        return new ToolExecutionResult(content, structured, false);
    }
}
