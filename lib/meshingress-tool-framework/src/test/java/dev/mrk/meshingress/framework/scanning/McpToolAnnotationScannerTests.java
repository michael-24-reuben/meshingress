package dev.mrk.meshingress.framework.scanning;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.progress.McpProgressReporter;
import dev.mrk.meshingress.api.tools.ToolVisibility;
import dev.mrk.meshingress.api.tools.annotation.*;
import dev.mrk.meshingress.api.tools.annotation.model.AnnotatedMcpTool;
import dev.mrk.meshingress.scopes.McpToolScope;
import dev.mrk.meshingress.tools.availability.enableondays.EnableOnDays;
import dev.mrk.meshingress.tools.availability.featureflag.EnableWhenFeatureFlagOn;
import dev.mrk.meshingress.tools.availability.withintimeranges.EnableWithinTimeRanges;
import dev.mrk.meshingress.tools.framework.scanning.McpToolAnnotationScanner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpToolAnnotationScannerTests {

    private final McpToolAnnotationScanner scanner = new McpToolAnnotationScanner(new ObjectMapper(), "test");

    @Test
    void scansToolMetadataAndDefaultFunctionSchema() {
        AnnotatedMcpTool tool = scanner.scan(HelloWorldAnnotatedTool.class);

        assertEquals("test.helloworld", tool.descriptor().name());
        assertEquals("Hello World", tool.descriptor().label());
        assertEquals(ToolVisibility.PUBLIC, tool.descriptor().visibility());
        assertEquals("LOCAL_READ", tool.descriptor().annotations().path("scopes").get(0).asString());
        assertEquals("EXTERNAL_API_READ", tool.functions().getFirst().annotations().path("scopes").get(0).asString());

        assertEquals(1, tool.functions().size());
        assertEquals("call", tool.functions().getFirst().name());
        assertEquals("test.helloworld.call", tool.functions().getFirst().descriptor().name());
        assertEquals("test.helloworld.call", tool.functions().getFirst().descriptor().handlerKey());
        assertEquals("Invoke the tool with the provided arguments.", tool.functions().getFirst().description());
        assertEquals("object", tool.functions().getFirst().inputSchema().path("type").asString());
        assertEquals(
                "The name of the person to greet.",
                tool.functions().getFirst().inputSchema().path("properties").path("name").path("description").asString()
        );
        assertEquals(
                "Greeting target",
                tool.functions().getFirst().inputSchema().path("properties").path("name").path("title").asString()
        );
        assertEquals("name", tool.functions().getFirst().inputSchema().path("required").get(0).asString());
    }

    @Test
    void infersSingleArgsObjectParameterWithoutFunctionParamAnnotation() {
        AnnotatedMcpTool tool = scanner.scan(InferredArgsAnnotatedTool.class);

        assertEquals(1, tool.functions().getFirst().parameters().size());
        assertEquals("args", tool.functions().getFirst().parameters().getFirst().name());
        assertEquals(HelloWorldGreetArgs.class, tool.functions().getFirst().parameters().getFirst().bindType());
        assertEquals("name", tool.functions().getFirst().descriptor().inputSchema().path("required").get(0).asString());
    }

    @Test
    void usesMethodInputSchemaDescriptionForDefaultProvider() {
        AnnotatedMcpTool tool = scanner.scan(MethodSchemaDescriptionAnnotatedTool.class);

        assertEquals(
                "Method-level input schema description.",
                tool.functions().getFirst().descriptor().inputSchema().path("description").asString()
        );
    }

    @Test
    void rejectsDotsInsideToolAndFunctionSegments() {
        IllegalStateException toolException = assertThrows(IllegalStateException.class,
                () -> scanner.scan(DottedToolAnnotatedTool.class));
        assertEquals("Mcp tool 'dev.mrk.meshingress.framework.scanning.McpToolAnnotationScannerTests$DottedToolAnnotatedTool' requires valid @McpTool annotation: 'dotted.tool'", toolException.getMessage());

        IllegalStateException functionException = assertThrows(IllegalStateException.class,
                () -> scanner.scan(DottedFunctionAnnotatedTool.class));
        assertEquals("MCP function name must use one lower-case segment: dotted.function", functionException.getMessage());
    }

    @Test
    void excludesProgressReporterFromFunctionParametersAndInputSchema() {
        AnnotatedMcpTool tool = scanner.scan(ProgressAwareAnnotatedTool.class);

        assertEquals(1, tool.functions().getFirst().parameters().size());
        assertEquals("args", tool.functions().getFirst().parameters().getFirst().name());
        assertEquals(HelloWorldGreetArgs.class, tool.functions().getFirst().parameters().getFirst().bindType());
        assertEquals(1, tool.functions().getFirst().inputSchema().path("properties").size());
        assertEquals(true, tool.functions().getFirst().descriptor().annotations().path("progressReporter").asBoolean());
        assertEquals("string", tool.functions().getFirst().inputSchema().path("properties").path("name").path("type").asString());
    }

    @Test
    void compilesAvailabilityAnnotationsIntoTheFunctionDescriptor() {
        JsonNode availability = scanner.scan(AvailableAnnotatedTool.class)
                .functions().getFirst().descriptor()
                .toMcpJson(new ObjectMapper())
                .path("annotations")
                .path("availability");

        assertEquals(1, availability.path("version").asInt());
        assertEquals("any", availability.path("mode").asString());
        assertEquals("dev.mrk.availability.day-of-week", availability.path("conditions").get(0).path("type").asString());
        assertEquals("MONDAY", availability.path("conditions").get(0).path("parameters").path("days").get(0).asString());
        assertEquals("dev.mrk.availability.feature-flag", availability.path("conditions").get(1).path("type").asString());
        assertEquals("experimental-search", availability.path("conditions").get(1).path("parameters").path("flag").asString());
        assertEquals("dev.mrk.availability.time-ranges", availability.path("conditions").get(2).path("type").asString());
        assertEquals("America/New_York", availability.path("conditions").get(2).path("parameters").path("timeZone").asString());
        assertEquals("09:00", availability.path("conditions").get(2).path("parameters").path("ranges").get(0).path("start").asString());
        assertEquals("17:00", availability.path("conditions").get(2).path("parameters").path("ranges").get(0).path("end").asString());
    }

    @Test
    void compilesTypedConstraintsAndGenericCollectionItemsIntoInputSchema() {
        ObjectNode properties = scanner.scan(ConstrainedAnnotatedTool.class)
                .functions().getFirst().inputSchema().withObject("properties");

        assertEquals("array", properties.path("genres").path("type").asString());
        assertEquals("string", properties.path("genres").path("items").path("type").asString());
        assertEquals(1, properties.path("genres").path("minItems").asInt());
        assertEquals(true, properties.path("genres").path("uniqueItems").asBoolean());

        assertEquals(1, properties.path("limit").path("minimum").asInt());
        assertEquals(100, properties.path("limit").path("maximum").asInt());
        assertEquals(20, properties.path("limit").path("default").asInt());
        assertEquals(0.0, properties.path("rating").path("minimum").asDouble());
        assertEquals(5.0, properties.path("rating").path("maximum").asDouble());

        assertEquals("base64", properties.path("file").path("contentEncoding").asString());
        assertEquals("application/pdf", properties.path("file").path("contentMediaType").asString());
        assertEquals("image/png", properties.path("image").path("contentMediaType").asString());
    }

    @Test
    void rejectsAConstraintWhoseDeclaredJavaTypeDoesNotMatch() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> scanner.scan(MismatchedConstraintAnnotatedTool.class));

        assertEquals("@McpNumberConstraints requires a non-integral numeric Java type, not java.lang.Integer", exception.getMessage());
    }

    @Test
    void compilesTheRemainingObjectTypeConstraintsIntoSchemaMetadata() {
        ObjectNode properties = scanner.scan(RemainingConstrainedAnnotatedTool.class)
                .functions().getFirst().inputSchema().withObject("properties");

        assertEquals(2, properties.path("text").path("minLength").asInt());
        assertEquals("^[A-Z].*", properties.path("text").path("pattern").asString());
        assertEquals("Hello", properties.path("text").path("default").asString());
        assertEquals("json", properties.path("json").path("format").asString());
        assertEquals(1, properties.path("json").path("minProperties").asInt());
        assertEquals(true, properties.path("bool").path("default").asBoolean());
        assertEquals("date", properties.path("date").path("format").asString());
        assertEquals("2026-01-01", properties.path("date").path("x-mcp-formatMinimum").asString());
        assertEquals("binary", properties.path("binary").path("format").asString());
        assertEquals(512, properties.path("binary").path("x-mcp-maxBytes").asInt());
        assertEquals("null", properties.path("nothing").path("type").asString());
        assertEquals("uri", properties.path("url").path("format").asString());
        assertEquals("https", properties.path("url").path("x-mcp-allowedSchemes").get(0).asString());
        assertEquals("unknown", properties.path("unknown").path("format").asString());
        assertEquals("password", properties.path("secret").path("format").asString());
        assertEquals("SUNNY", properties.path("style").path("default").asString());
        assertEquals("code", properties.path("code").path("format").asString());
        assertEquals("java", properties.path("code").path("x-mcp-language").asString());
        assertEquals("email", properties.path("email").path("format").asString());
        assertEquals("example.com", properties.path("email").path("x-mcp-allowedDomains").get(0).asString());
        assertEquals(0.01, properties.path("decimal").path("minimum").decimalValue().doubleValue());
        assertEquals("clock", properties.path("clock").path("format").asString());
        assertEquals("time", properties.path("time").path("format").asString());
        assertEquals("duration", properties.path("duration").path("format").asString());
        assertEquals("interval", properties.path("interval").path("format").asString());
        assertEquals("color", properties.path("color").path("format").asString());
        assertEquals("hex", properties.path("color").path("x-mcp-allowedColorFormats").get(0).asString());
        assertEquals("location", properties.path("location").path("format").asString());
        assertEquals("US", properties.path("location").path("x-mcp-allowedCountryCodes").get(0).asString());
        assertEquals("geo", properties.path("geo").path("format").asString());
        assertEquals(-45.0, properties.path("geo").path("x-mcp-minLatitude").asDouble());
    }

    @McpTool(
            value = "helloworld",
            title = "Hello World",
            description = "Return a greeting from an external Meshingress tool module."
    )
    @McpToolScopes(McpToolScope.LOCAL_READ)
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
            @McpInputField(title = "Greeting target", description = "The name of the person to greet.")
            String name
    ) {
    }

    @McpTool(value = "inferred")
    static class InferredArgsAnnotatedTool {

        @McpFunction("call")
        ObjectNode call(HelloWorldGreetArgs arguments, McpCallContext context) {
            return null;
        }
    }

    @McpTool(value = "schema")
    static class MethodSchemaDescriptionAnnotatedTool {

        @McpFunction(value = "call", description = "Function fallback description.")
        @McpInputSchema(description = "Method-level input schema description.")
        ObjectNode call(@McpFunctionParam("name") String name) {
            return null;
        }
    }

    @McpTool(value = "progress")
    static class ProgressAwareAnnotatedTool {

        @McpFunction("call")
        ObjectNode call(HelloWorldGreetArgs arguments, McpCallContext context, McpProgressReporter progress) {
            return null;
        }
    }

    @McpTool(value = "availability")
    static class AvailableAnnotatedTool {

        @EnableOnDays({DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY})
        @EnableWhenFeatureFlagOn("experimental-search")
        @EnableWithinTimeRanges(
                zone = "America/New_York",
                ranges = @EnableWithinTimeRanges.TimeRange(start = "09:00", end = "17:00")
        )
        @McpConfigureMapping(availabilityMode = McpAvailabilityMode.ANY)
        @McpFunction("call")
        ObjectNode call() {
            return null;
        }
    }

    @McpTool(value = "constraints")
    static class ConstrainedAnnotatedTool {

        @McpFunction("call")
        ObjectNode call(ConstrainedArgs arguments) {
            return null;
        }
    }

    record ConstrainedArgs(
            @McpInputField(required = false)
            @McpArrayConstraints(minItems = 1, uniqueItems = true)
            List<String> genres,
            @McpInputField(required = false)
            @McpIntegerConstraints(minimum = 1, maximum = 100, defaultValue = 20)
            Integer limit,
            @McpInputField(required = false)
            @McpNumberConstraints(minimum = 0, maximum = 5)
            Double rating,
            @McpInputField(required = false)
            @McpFileConstraints(contentEncoding = "base64", contentMediaType = "application/pdf")
            String file,
            @McpInputField(required = false)
            @McpImageConstraints(contentMediaType = "image/png")
            String image
    ) {
    }

    @McpTool(value = "invalid")
    static class MismatchedConstraintAnnotatedTool {

        @McpFunction("call")
        ObjectNode call(MismatchedConstraintArgs arguments) {
            return null;
        }
    }

    record MismatchedConstraintArgs(
            @McpInputField
            @McpNumberConstraints(minimum = 0)
            Integer limit
    ) {
    }

    @McpTool(value = "remaining")
    static class RemainingConstrainedAnnotatedTool {

        @McpFunction("call")
        ObjectNode call(RemainingConstrainedArgs arguments) {
            return null;
        }
    }

    @McpTool(value = "dotted.tool")
    static class DottedToolAnnotatedTool {
        @McpFunction("call")
        ObjectNode call() { return null; }
    }

    @McpTool(value = "valid")
    static class DottedFunctionAnnotatedTool {
        @McpFunction("dotted.function")
        ObjectNode call() { return null; }
    }

    record RemainingConstrainedArgs(
            @McpInputField(required = false)
            @McpTextConstraints(minLength = 2, pattern = "^[A-Z].*", defaultValue = "Hello", hasDefault = true)
            String text,
            @McpInputField(required = false)
            @McpJsonConstraints(minProperties = 1)
            JsonNode json,
            @McpInputField(required = false)
            @McpBooleanConstraints(defaultValue = true, hasDefault = true)
            Boolean bool,
            @McpInputField(required = false)
            @McpDateConstraints(minimum = "2026-01-01")
            String date,
            @McpInputField(required = false)
            @McpBinaryConstraints(contentEncoding = "base64", maxBytes = 512)
            String binary,
            @McpInputField(required = false, schemaType = "null")
            @McpNullConstraints
            JsonNode nothing,
            @McpInputField(required = false)
            @McpUrlConstraints(allowedSchemes = "https")
            String url,
            @McpInputField(required = false)
            @McpUnknownConstraints(allowedSchemaTypes = {"string", "object"})
            Object unknown,
            @McpInputField(required = false)
            @McpSecretConstraints(minLength = 12)
            String secret,
            @McpInputField(required = false)
            @McpEnumConstraints(defaultValue = "SUNNY")
            GreetingStyle style,
            @McpInputField(required = false)
            @McpCodeConstraints(language = "java", minLength = 1)
            String code,
            @McpInputField(required = false)
            @McpEmailConstraints(allowedDomains = "example.com")
            String email,
            @McpInputField(required = false)
            @McpDecimalConstraints(minimum = "0.01", multipleOf = "0.01")
            BigDecimal decimal,
            @McpInputField(required = false)
            @McpClockConstraints(minimum = "09:00")
            String clock,
            @McpInputField(required = false)
            @McpTimeConstraints(maximum = "17:00:00")
            String time,
            @McpInputField(required = false)
            @McpDurationConstraints(minimum = "PT5M")
            String duration,
            @McpInputField(required = false)
            @McpIntervalConstraints(maximumDuration = "PT1H")
            String interval,
            @McpInputField(required = false)
            @McpColorConstraints(allowedFormats = "hex")
            String color,
            @McpInputField(required = false)
            @McpLocationConstraints(allowedCountryCodes = "US")
            JsonNode location,
            @McpInputField(required = false)
            @McpGeoConstraints(minimumLatitude = -45)
            JsonNode geo
    ) {
    }

    enum GreetingStyle {
        SUNNY
    }
}
