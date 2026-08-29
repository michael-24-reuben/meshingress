package dev.mrk.toolspace.helloworld;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.result.ResultContent;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.dispatch.text.PlainTextContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.tools.availability.withintimeranges.EnableWithinTimeRanges;

import java.util.ArrayList;
import java.util.List;

@McpTool(
        value = "announce",
        title = "Hello World",
        description = "Return a greeting assembled from an expansive, optional input-schema example."
)
@McpToolScopes(McpToolScope.USER_WRITE)
public class HelloWorldTool {

    @EnableWithinTimeRanges(
            ranges = {
                    @EnableWithinTimeRanges.TimeRange(start = "06:00", end = "12:00")
            },
            zone = "America/New_York"
    )
    @McpConfigureMapping(
            timeoutMs = 20_000,
            availabilityMode = McpAvailabilityMode.ANY
    )
    @McpFunction(value = "good_morning", description = "Wish someone a good morning.")
    public DispatchExecutionResult goodMorning(McpCallContext context) {
        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.text("Good morning!"))
                .structuredContent(new PlainTextContent("Good morning!"))
                .error(false)
                .build();
    }
    /*
    * - @McpCallContext(services={@McpCallContext.Service(type=ToolStorageService.class, oneof={classes})})
    * */

    @McpConfigureMapping(timeoutMs = 20_000)
    @McpFunction(value = "greetings", description = "Greet a person with optional tone, context, and presentation details.")
    public DispatchExecutionResult call(HelloWorldGreetArgs arguments, McpCallContext context) {
        String greeting = greetingFor(arguments);
        PlainTextContent structured = new PlainTextContent(greeting);
        structured.setTitle("Greeting");

        return DispatchExecutionResult.builder()
                .appendContent(ResultContent.text(greeting))
                .structuredContent(structured)
                .error(false)
                .build();
    }

    private String greetingFor(HelloWorldGreetArgs arguments) {
        String opening = firstText(arguments.text, defaultOpening(arguments.booleanValue, arguments.enumValue));
        String audience = audienceFor(arguments.name, arguments.array);
        List<String> details = new ArrayList<>();

        add(details, arguments.json != null, "with a persona card");
        add(details, hasText(arguments.date), "for " + arguments.date);
        add(details, hasText(arguments.binary), "sealed in binary");
        add(details, arguments.nullValue != null && arguments.nullValue.isNull(), "with a deliberate silent beat");
        add(details, hasText(arguments.url), "inspired by " + arguments.url);
        add(details, hasText(arguments.file), "with attachment " + arguments.file);
        add(details, arguments.unknown != null, "with one mystery detail");
        add(details, hasText(arguments.secret), "under a private sign-off");
        add(details, hasText(arguments.code), "following a code cue");
        add(details, hasText(arguments.email), "reply via " + arguments.email);
        add(details, arguments.decimal != null, "with warmth " + arguments.decimal.toPlainString());
        add(details, hasText(arguments.image), "against an image backdrop");
        add(details, hasText(arguments.clock), "at " + arguments.clock + " on the clock");
        add(details, hasText(arguments.time), "for " + arguments.time);
        add(details, hasText(arguments.duration), "for " + arguments.duration);
        add(details, hasText(arguments.interval), "during " + arguments.interval);
        add(details, hasText(arguments.color), "in " + arguments.color);
        add(details, arguments.location != null, "from a named location");
        add(details, arguments.geo != null, "with a geo pin");

        String punctuation = "!".repeat(Math.clamp(arguments.number == null ? 1 : arguments.number, 1, 5));
        String suffix = details.isEmpty() ? "" : " " + String.join(", ", details) + ".";
        return opening + ", " + audience + punctuation + suffix;
    }

    private String defaultOpening(Boolean formal, HelloWorldGreetArgs.GreetingStyle style) {
        if (style == HelloWorldGreetArgs.GreetingStyle.cosmic) return "Greetings, star traveler";
        if (style == HelloWorldGreetArgs.GreetingStyle.pirate) return "Ahoy";
        if (style == HelloWorldGreetArgs.GreetingStyle.understated) return "Hello";
        if (style == HelloWorldGreetArgs.GreetingStyle.sunny) return "Bright greetings";
        return Boolean.TRUE.equals(formal) ? "Good day" : "Hello";
    }

    private String audienceFor(String name, List<String> audience) {
        if (audience == null || audience.isEmpty()) return name;
        List<String> names = new ArrayList<>();
        names.add(name);
        audience.stream().filter(HelloWorldTool::hasText).forEach(names::add);
        return String.join(" and ", names);
    }

    private static void add(List<String> details, boolean condition, String detail) {
        if (condition) details.add(detail);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
