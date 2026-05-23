package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.ObjectMapper;

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
            timeoutMs = 20_000
    )
    @McpFunction(value = "call", description = "Greet the Person.")
    public DispatchExecutionResult call(HelloWorldGreetArgs arguments, McpCallContext context) {
        String name = arguments.getName();
        DispatchExecutionResult.Builder dispatch = DispatchExecutionResult.builder()
                .appendContent(ResultContent.text("Hello, " + name + "!"))
                .structuredContent(objectMapper.createObjectNode().put("message", "Hello, " + name + "!"))
                .error(false);

        return dispatch.build();
    }
}
