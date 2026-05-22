package dev.mrk.meshingress.framework;

import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityCondition;
import dev.mrk.meshingress.route.annotations.*;
import dev.mrk.meshingress.api.tools.annotation.availability.AvailabilityValidationContext;
import dev.mrk.meshingress.route.api.*;
import dev.mrk.meshingress.api.tools.annotation.McpToolAvailabilityCondition;
//import dev.mrk.meshingress.route.annotations.availability.RouteAvailabilityCondition;
//import dev.mrk.meshingress.route.availability.featureflag.EnableWhenFeatureFlagOn;
//import dev.mrk.meshingress.route.availability.featureflag.FeatureFlagOnCondition;
//import dev.mrk.meshingress.route.availability.policy.EnableWhenFeatureFlagOnPolicy;
//import dev.mrk.meshingress.route.availability.withintimeranges.EnableWithinTimeRanges;
import dev.mrk.meshingress.api.tools.annotation.McpAvailabilityMode;
import dev.mrk.meshingress.route.framework.*;
import dev.mrk.meshingress.tools.availability.enableondays.EnableOnDaysCondition;
import dev.mrk.meshingress.tools.availability.featureflag.EnableWhenFeatureFlagOn;
import dev.mrk.meshingress.tools.availability.featureflag.FeatureFlagOnCondition;
import dev.mrk.meshingress.tools.availability.policy.EnableWhenFeatureFlagOnPolicy;
import dev.mrk.meshingress.tools.availability.withintimeranges.EnableWithinTimeRanges;
import dev.mrk.meshingress.tools.availability.withintimeranges.WithinTimeRangesCondition;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class McpRouteFrameworkTests {

    private final McpRouteAnnotationScanner scanner = new McpRouteAnnotationScanner();

    @Test
    void scansAndValidatesAnnotatedRouteMetadata() {
        List<AnnotatedMcpRoute> routes = scanner.scan(List.of(InstagramRouteController.class));

        try (AnnotationConfigApplicationContext context = validationContext()) {
            new McpRouteValidator(context).validateOrThrow(routes);
        }
        AnnotatedMcpRoute route = routes.getFirst();
        McpRouteRegistry registry = new McpRouteRegistry(routes);

        assertEquals("instagram.publish.post.v1", route.routeId());
        assertEquals(McpHttpMethod.POST, route.route().method());
        assertEquals("/mcp/tools/instagram/publish/{workspaceId}", route.route().path());
        assertEquals(McpAvailabilityMode.ALL, route.configuration().availabilityMode());
        assertEquals("instagramApiToken", route.configuration().secrets()[0].name());
        assertEquals(1, route.middlewareTypes().size());
        assertEquals(1, route.availabilityAnnotations().size());
        assertTrue(registry.findById("instagram.publish.post.v1").isPresent());
    }

    @Test
    void rejectsInvalidHandlerSignaturesAtValidationTime() {
        List<AnnotatedMcpRoute> routes = scanner.scan(List.of(InvalidRouteController.class));

        McpRouteValidationException exception;
        try (AnnotationConfigApplicationContext context = validationContext()) {
            exception = assertThrows(McpRouteValidationException.class, () -> new McpRouteValidator(context).validateOrThrow(routes));
        }

        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("must accept exactly one HTTPRequest")));
        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("must return HTTPResponse")));
    }

    @Test
    void rejectsDuplicateRouteIds() {
        List<AnnotatedMcpRoute> routes = scanner.scan(List.of(DuplicateRouteController.class));

        McpRouteValidationException exception;
        try (AnnotationConfigApplicationContext context = validationContext()) {
            exception = assertThrows(McpRouteValidationException.class, () -> new McpRouteValidator(context).validateOrThrow(routes));
        }

        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("Duplicate MCP route ID")));
    }

    @Test
    void rejectsAvailabilityMetadataProblemsCollectively() {
        List<AnnotatedMcpRoute> routes = scanner.scan(List.of(InvalidAvailabilityRouteController.class));

        McpRouteValidationException exception;
        try (AnnotationConfigApplicationContext context = validationContext()) {
            exception = assertThrows(McpRouteValidationException.class, () -> new McpRouteValidator(context).validateOrThrow(routes));
        }

        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("invalid time zone")));
        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("invalid time range")));
        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("start must be before end")));
    }

    @Test
    void rejectsBlankFeatureFlagNamesInConditionValidation() throws Exception {
        EnableWhenFeatureFlagOn annotation = blankFeatureFlagAnnotation();
        Method method = AlwaysAvailableRouteController.class.getDeclaredMethod("post", HTTPRequest.class);

        List<String> violations = new FeatureFlagOnCondition().validate(
                annotation,
                new AvailabilityValidationContext(AlwaysAvailableRouteController.class, method)
        );

        assertTrue(violations.stream().anyMatch(value -> value.contains("blank feature flag name")));
    }

    @Test
    void rejectsAvailabilityAnnotationsMissingConditionDeclaration() {
        List<AnnotatedMcpRoute> routes = scanner.scan(List.of(MissingConditionDeclarationController.class));

        McpRouteValidationException exception;
        try (AnnotationConfigApplicationContext context = validationContext()) {
            exception = assertThrows(McpRouteValidationException.class, () -> new McpRouteValidator(context).validateOrThrow(routes));
        }

        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("missing @McpToolAvailabilityCondition")));
    }

    @Test
    void rejectsAvailabilityConditionsMissingSpringBeans() {
        List<AnnotatedMcpRoute> routes = scanner.scan(List.of(MissingBeanConditionController.class));

        McpRouteValidationException exception;
        try (AnnotationConfigApplicationContext context = validationContext()) {
            exception = assertThrows(McpRouteValidationException.class, () -> new McpRouteValidator(context).validateOrThrow(routes));
        }

        assertTrue(exception.violations().stream().anyMatch(value -> value.contains("not registered as a Spring bean")));
    }

    @Test
    void middlewarePipelineRunsInProvidedOrder() {
        HTTPRequest<Query, Params, Body> request = HTTPRequest.of(new Query("public"), new Params("workspace-1"), new Body("caption"));
        McpRouteExecutionContext context = McpRouteExecutionContext.forRoute("instagram.publish.post.v1");
        McpMiddlewareExecutor executor = new McpMiddlewareExecutor();

        HTTPRequest<Query, Params, Body> result = executor.execute(
                request,
                context,
                List.of(new AddFirstMiddleware(), new AddSecondMiddleware())
        );

        assertEquals("first", result.attributes().get("first"));
        assertEquals("second", result.attributes().get("second"));
    }

    @Test
    void availabilityEvaluatorUsesFeatureFlagPolicy() {
        AnnotatedMcpRoute route = scanner.scan(List.of(InstagramRouteController.class)).getFirst();
        McpAvailabilityEvaluator evaluator = new McpAvailabilityEvaluator();
        HTTPRequest<Query, Params, Body> request = HTTPRequest.of(new Query("public"), new Params("workspace-1"), new Body("caption"));
        McpRouteExecutionContext enabledContext = McpRouteExecutionContext.forRoute(route.routeId()).withAttribute(
                EnableWhenFeatureFlagOnPolicy.FEATURE_FLAG_READER_ATTRIBUTE,
                (EnableWhenFeatureFlagOnPolicy.FeatureFlagReader) (flagName, context) -> {
                    Objects.requireNonNull(flagName);
                    Objects.requireNonNull(context);
                    return true;
                }
        );
        McpRouteExecutionContext disabledContext = McpRouteExecutionContext.forRoute(route.routeId()).withAttribute(
                EnableWhenFeatureFlagOnPolicy.FEATURE_FLAG_READER_ATTRIBUTE,
                (EnableWhenFeatureFlagOnPolicy.FeatureFlagReader) (flagName, context) -> {
                    Objects.requireNonNull(flagName);
                    Objects.requireNonNull(context);
                    return false;
                }
        );

        AvailabilityDecision enabled = evaluator.evaluate(route, request, enabledContext);
        AvailabilityDecision disabled = evaluator.evaluate(route, request, disabledContext);

        assertTrue(enabled.allowed());
        assertFalse(disabled.allowed());
    }

    @Test
    void executionPipelineInjectsServerRouteIdAndEmitsLifecycleEvents() {
        AnnotatedMcpRoute route = scanner.scan(List.of(AlwaysAvailableRouteController.class)).getFirst();
        List<McpRouteLifecycleEvent> events = new ArrayList<>();
        McpRouteExecutionPipeline pipeline = new McpRouteExecutionPipeline(
                new McpMiddlewareExecutor(),
                new McpAvailabilityEvaluator(),
                new StandardMcpRouteErrorMapper(),
                events::add
        );
        HTTPRequest<Query, Params, Body> request = HTTPRequest.of(new Query("public"), new Params("workspace-1"), new Body("caption"));

        HTTPResponse<Response> response = pipeline.execute(
                route,
                request,
                McpRouteExecutionContext.forRoute(route.routeId()),
                List.of(new AddFirstMiddleware()),
                (guardedRequest, context) -> {
                    Objects.requireNonNull(context);
                    return HTTPResponse.ok(new Response(guardedRequest.routeId()));
                }
        );

        assertEquals(200, response.status());
        assertEquals("always.available.route.v1", response.body().result());
        assertEquals("route.execution.started", events.get(0).name());
        assertEquals("route.execution.completed", events.get(1).name());
    }

    @SuppressWarnings("unused")
    static class InstagramRouteController {

        @McpRoute(
                id = "instagram.publish.post.v1",
                method = McpHttpMethod.POST,
                path = "/mcp/tools/instagram/publish/{workspaceId}"
        )
        @McpRequestMiddleware(AddFirstMiddleware.class)
        @McpConfigureMapping(
                secrets = @McpSecret(name = "instagramApiToken", ref = "instagram-api-token"),
                availabilityMode = McpAvailabilityMode.ALL,
                audit = true,
                debugTrace = true,
                timeoutMs = 20_000
        )
        @EnableWhenFeatureFlagOn("instagram.publish.enabled")
        HTTPResponse<Response> post(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("ok"));
        }
    }

    @SuppressWarnings("unused")
    static class InvalidRouteController {

        @McpRoute(id = "invalid.route.v1", method = McpHttpMethod.POST, path = "/invalid")
        String post(String request) {
            return "invalid";
        }
    }

    @SuppressWarnings("unused")
    static class DuplicateRouteController {

        @McpRoute(id = "duplicate.route.v1", method = McpHttpMethod.POST, path = "/one")
        HTTPResponse<Response> one(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("one"));
        }

        @McpRoute(id = "duplicate.route.v1", method = McpHttpMethod.POST, path = "/two")
        HTTPResponse<Response> two(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("two"));
        }
    }

    @SuppressWarnings("unused")
    static class AlwaysAvailableRouteController {

        @McpRoute(id = "always.available.route.v1", method = McpHttpMethod.POST, path = "/always")
        HTTPResponse<Response> post(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("ok"));
        }
    }

    @SuppressWarnings("unused")
    static class InvalidAvailabilityRouteController {

        @McpRoute(id = "availability.invalid.route.v1", method = McpHttpMethod.POST, path = "/availability/invalid")
        @EnableWithinTimeRanges(zone = "not-a-zone", ranges = {"09:00/17:00", "18:00-17:00"})
        HTTPResponse<Response> post(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("ok"));
        }
    }

    @SuppressWarnings("unused")
    static class MissingConditionDeclarationController {

        @McpRoute(id = "availability.missing.condition.v1", method = McpHttpMethod.POST, path = "/availability/missing-condition")
        @MissingConditionAnnotation
        HTTPResponse<Response> post(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("ok"));
        }
    }

    @SuppressWarnings("unused")
    static class MissingBeanConditionController {

        @McpRoute(id = "availability.missing.bean.v1", method = McpHttpMethod.POST, path = "/availability/missing-bean")
        @MissingBeanAnnotation
        HTTPResponse<Response> post(HTTPRequest<Query, Params, Body> request) {
            return HTTPResponse.ok(new Response("ok"));
        }
    }

    static class AddFirstMiddleware implements McpMiddleware<Query, Params, Body> {

        @Override
        public HTTPRequest<Query, Params, Body> apply(
                HTTPRequest<Query, Params, Body> request,
                McpRouteExecutionContext context
        ) {
            return request.withAttribute("first", "first");
        }
    }

    static class AddSecondMiddleware implements McpMiddleware<Query, Params, Body> {

        @Override
        public HTTPRequest<Query, Params, Body> apply(
                HTTPRequest<Query, Params, Body> request,
                McpRouteExecutionContext context
        ) {
            return request.withAttribute("second", "second");
        }
    }

    private AnnotationConfigApplicationContext validationContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(WithinTimeRangesCondition.class);
        context.registerBean(FeatureFlagOnCondition.class);
        context.registerBean(EnableOnDaysCondition.class);
        context.refresh();
        return context;
    }

    private EnableWhenFeatureFlagOn blankFeatureFlagAnnotation() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "value" -> "";
            case "annotationType" -> EnableWhenFeatureFlagOn.class;
            case "toString" -> "@EnableWhenFeatureFlagOn(value=\"\")";
            case "hashCode" -> 0;
            case "equals" -> proxy == args[0];
            default -> method.getDefaultValue();
        };
        return (EnableWhenFeatureFlagOn) Proxy.newProxyInstance(
                EnableWhenFeatureFlagOn.class.getClassLoader(),
                new Class<?>[]{EnableWhenFeatureFlagOn.class},
                handler
        );
    }

    @McpAvailabilityPolicy(AllowAllAvailabilityPolicy.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MissingConditionAnnotation {
    }

    @McpAvailabilityPolicy(AllowAllAvailabilityPolicy.class)
    @McpToolAvailabilityCondition(MissingBeanCondition.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MissingBeanAnnotation {
    }

    static final class AllowAllAvailabilityPolicy implements AvailabilityPolicy<Annotation> {

        @Override
        public AvailabilityDecision evaluate(Annotation annotation, HTTPRequest<?, ?, ?> request, McpRouteExecutionContext context) {
            return AvailabilityDecision.allow();
        }
    }

    static final class MissingBeanCondition implements AvailabilityCondition<MissingBeanAnnotation> {

        @Override
        public List<String> validate(MissingBeanAnnotation annotation, AvailabilityValidationContext context) {
            return List.of();
        }

    }

    record Query(String visibility) {
    }

    record Params(String workspaceId) {
    }

    record Body(String caption) {
    }

    record Response(String result) {
    }
}
