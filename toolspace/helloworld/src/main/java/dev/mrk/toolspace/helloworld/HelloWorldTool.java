package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheKeyMode;
import dev.mrk.meshingress.api.tools.annotation.cache.McpCacheStorage;
import dev.mrk.meshingress.dispatch.text.PlainTextContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.tools.availability.enableondays.EnableOnDays;

import java.time.DayOfWeek;

@McpTool(
        value = "helloworld",
        title = "Hello World",
        description = "Return a greeting from an external Meshingress tool module."
)
@McpToolScopes(McpToolScope.USER_WRITE)
@McpToolMapping("tools")
public class HelloWorldTool {

    @McpCacheResult(
            enabled = true,
            ttlMs = 60_000L,
            namespace = "helloworld",
            keyPrefix = "greet",
            includeArguments = {"name"},
            includeToolId = true,
            includeFunctionName = true,
            includePrincipal = false,
            includeSession = false,
            cacheErrors = false,
            cacheEmptyResults = true,
            keyMode = McpCacheKeyMode.CANONICAL_ARGUMENTS,
            storage = McpCacheStorage.DEFAULT
    )
//    @EnableOnDays({DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY})
    @McpConfigureMapping(
            timeoutMs = 20_000
    )
    @McpFunction(value = "greet", description = "Greet the Person.")
    public DispatchExecutionResult call(HelloWorldGreetArgs arguments, McpCallContext context) {
        String name = arguments.getName();
        PlainTextContent structured = new PlainTextContent("Hello, " + name + "!");
        structured.setTitle("Greeting");

        DispatchExecutionResult.Builder dispatch = DispatchExecutionResult.builder()
                .appendContent(ResultContent.text("Hello, " + name + "!"))
                .structuredContent(structured)
                .error(false);

        return dispatch.build();
    }
}
