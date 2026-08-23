package dev.mrk.meshingress.workflow;

import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.dispatch.process.ProcessExecutionContent;
import dev.mrk.meshingress.mcp.tools.ToolExecutor;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowRuntimeTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowCompiler compiler = new WorkflowCompiler();

    @Test
    void routesStringResultToItsMatchingCaseAndBindsTheNamedResult() {
        ToolExecutor executor = (functionName, arguments, context) -> {
            assertThat(functionName).isEqualTo("vehicle.lookup");
            assertThat(context.lineage().workflowRunId()).startsWith("run_");
            assertThat(context.lineage().workflowNodeId()).isEqualTo("req-vehicle");
            assertThat(context.lineage().workflowAttempt()).isEqualTo(1);
            return DispatchExecutionResult.builder().text("gray toyota").build();
        };
        WorkflowRuntime runtime = new WorkflowRuntime(compiler, executor, objectMapper);
        WorkflowDefinition definition = new WorkflowDefinition("vehicle-report", List.of(
                new WorkflowNode.ManualTrigger("req-trigger", nullResult("trigger")),
                new WorkflowNode.ToolCall(
                        "req-vehicle",
                        "vehicle.lookup",
                        Map.of(),
                        new WorkflowResultDeclaration(
                                "vehicle",
                                WorkflowResultDeclaration.WorkflowValueType.STRING,
                                new WorkflowRouting.Cases("", List.of(
                                        new WorkflowRouting.RouteCase(
                                                "grayToyota",
                                                new WorkflowRouting.StringMatches("(?i)gr(a|e)y\\s+toyota")
                                        )
                                ), "default")
                        ),
                        WorkflowFailurePolicy.failWorkflow()
                ),
                new WorkflowNode.Compose(
                        "req-report",
                        Map.of("vehicle", new WorkflowInput.ResultReference("vehicle", "")),
                        objectResult("report"),
                        WorkflowFailurePolicy.failWorkflow()
                )
        ), List.of(
                edge("req-trigger", "default", "req-vehicle"),
                edge("req-vehicle", "grayToyota", "req-report")
        ));

        WorkflowRun run = runtime.run(definition, null, null);

        assertThat(run.status()).isEqualTo(WorkflowRun.Status.COMPLETED);
        assertThat(run.results().get("vehicle").asString()).isEqualTo("gray toyota");
        assertThat(run.results().get("report").path("vehicle").asString()).isEqualTo("gray toyota");
        assertThat(run.nodeResults()).extracting(nodeResult -> nodeResult.outcome().port())
                .containsExactly("default", "grayToyota", "default");
    }

    @Test
    void joinWaitsForEveryInboundBranchAndExposesNamedResults() {
        ToolExecutor executor = (functionName, arguments, context) -> DispatchExecutionResult.builder()
                .text(functionName.equals("system.read") ? "system" : "weather")
                .build();
        WorkflowRuntime runtime = new WorkflowRuntime(compiler, executor, objectMapper);
        WorkflowDefinition definition = new WorkflowDefinition("join", List.of(
                new WorkflowNode.ManualTrigger("req-trigger", nullResult("trigger")),
                tool("req-system", "system.read", "system"),
                tool("req-weather", "weather.read", "weather"),
                new WorkflowNode.Join(
                        "req-join",
                        Map.of(
                                "system", new WorkflowInput.ResultReference("system", ""),
                                "weather", new WorkflowInput.ResultReference("weather", "")
                        ),
                        objectResult("combined")
                )
        ), List.of(
                edge("req-trigger", "default", "req-system"),
                edge("req-trigger", "default", "req-weather"),
                edge("req-system", "default", "req-join"),
                edge("req-weather", "default", "req-join")
        ));

        WorkflowRun run = runtime.run(definition, null, null);

        assertThat(run.status()).isEqualTo(WorkflowRun.Status.COMPLETED);
        assertThat(run.results().get("combined").path("system").asString()).isEqualTo("system");
        assertThat(run.results().get("combined").path("weather").asString()).isEqualTo("weather");
        assertThat(run.nodeResults()).extracting(WorkflowRun.NodeResult::nodeId)
                .contains("req-join");
    }

    @Test
    void followsTheNodeOwnedErrorPortAfterTheConfiguredAttemptFails() {
        ToolExecutor executor = (functionName, arguments, context) -> DispatchExecutionResult.builder()
                .error("TOOL_FAILURE", "tool is unavailable")
                .build();
        WorkflowRuntime runtime = new WorkflowRuntime(compiler, executor, objectMapper);
        WorkflowDefinition definition = new WorkflowDefinition("error-route", List.of(
                new WorkflowNode.ManualTrigger("req-trigger", nullResult("trigger")),
                new WorkflowNode.ToolCall(
                        "req-tool",
                        "missing.tool",
                        Map.of(),
                        stringResult("toolResult"),
                        new WorkflowFailurePolicy(2, "error", "toolFailure")
                ),
                new WorkflowNode.Compose(
                        "req-handle-error",
                        Map.of("message", new WorkflowInput.ResultReference("toolFailure", "/message")),
                        objectResult("handled"),
                        WorkflowFailurePolicy.failWorkflow()
                )
        ), List.of(
                edge("req-trigger", "default", "req-tool"),
                edge("req-tool", "error", "req-handle-error")
        ));

        WorkflowRun run = runtime.run(definition, null, null);

        assertThat(run.status()).isEqualTo(WorkflowRun.Status.COMPLETED);
        assertThat(run.results().get("toolFailure").path("attempts").asInt()).isEqualTo(2);
        assertThat(run.results().get("handled").path("message").asString()).isEqualTo("tool is unavailable");
        assertThat(run.nodeResults()).filteredOn(nodeResult -> nodeResult.outcome().failed())
                .singleElement()
                .satisfies(nodeResult -> assertThat(nodeResult.outcome().port()).isEqualTo("error"));
    }

    @Test
    void routesStructuredToolPayloadDataWhileRetainingTheEnvelopeInTheNodeRecord() {
        ToolExecutor executor = (functionName, arguments, context) -> {
            var payload = objectMapper.createObjectNode();
            payload.put("source", "toonverse");
            return DispatchExecutionResult.builder().structuredContent(payload).build();
        };
        WorkflowRuntime runtime = new WorkflowRuntime(compiler, executor, objectMapper);
        WorkflowDefinition definition = new WorkflowDefinition("structured-tool", List.of(
                new WorkflowNode.ManualTrigger("r-001", nullResult("trigger")),
                new WorkflowNode.ToolCall("r-002", "toonverse.search", Map.of(), objectResult("search"), WorkflowFailurePolicy.failWorkflow()),
                new WorkflowNode.Compose("r-003", Map.of("search", new WorkflowInput.ResultReference("search", "")), objectResult("compiled"), WorkflowFailurePolicy.failWorkflow())
        ), List.of(
                edge("r-001", "default", "r-002"),
                edge("r-002", "default", "r-003")
        ));

        WorkflowRun run = runtime.run(definition, null, null);

        assertThat(run.results().get("search").path("source").asString()).isEqualTo("toonverse");
        assertThat(run.results().get("compiled").path("search").path("source").asString()).isEqualTo("toonverse");
        assertThat(run.nodeResults()).filteredOn(nodeResult -> nodeResult.nodeId().equals("r-002"))
                .singleElement()
                .satisfies(nodeResult -> {
                    assertThat(nodeResult.nodePath()).isEqualTo("toonverse.search");
                    assertThat(nodeResult.variable()).isEqualTo("search");
                    assertThat(nodeResult.result().path("kind").asString()).isEqualTo("generated.json.object");
                    assertThat(nodeResult.result().path("data").path("source").asString()).isEqualTo("toonverse");
                    assertThat(nodeResult.outputSchema()).isNull();
                    assertThat(nodeResult.diagnostics()).isEmpty();
                    assertThat(nodeResult.startedAt()).isPositive();
                    assertThat(nodeResult.completedAt()).isGreaterThanOrEqualTo(nodeResult.startedAt());
                });
    }

    @Test
    void projectsTheTypedStructuredContentClassAsTheNodeOutputSchema() {
        ToolExecutor executor = (functionName, arguments, context) -> {
            ProcessExecutionContent content = new ProcessExecutionContent();
            content.setStatus("completed");
            content.setExitCode(0);
            return DispatchExecutionResult.builder().structuredContent(content).build();
        };
        WorkflowRuntime runtime = new WorkflowRuntime(compiler, executor, objectMapper);
        WorkflowDefinition definition = new WorkflowDefinition("typed-structured-tool", List.of(
                new WorkflowNode.ManualTrigger("r-001", nullResult("trigger")),
                new WorkflowNode.ToolCall("r-002", "cli.powershell.execute", Map.of(), objectResult("execution"), WorkflowFailurePolicy.failWorkflow())
        ), List.of(edge("r-001", "default", "r-002")));

        WorkflowRun run = runtime.run(definition, null, null);

        assertThat(run.nodeResults()).filteredOn(nodeResult -> nodeResult.nodeId().equals("r-002"))
                .singleElement()
                .satisfies(nodeResult -> {
                    assertThat(nodeResult.outputSchema()).isNotNull();
                    assertThat(nodeResult.outputSchema().path("$schema").asString())
                            .isEqualTo("https://json-schema.org/draft/2020-12/schema");
                    assertThat(nodeResult.outputSchema().at("/properties/kind/const").asString())
                            .isEqualTo("process.execution");
                    assertThat(nodeResult.outputSchema().at("/properties/data/properties/exitCode/type").asString())
                            .isEqualTo("integer");
                    assertThat(nodeResult.diagnostics()).isEmpty();
                });
    }

    @Test
    void rejectsObjectCaseRoutingWithoutAnExplicitTargetPointer() {
        WorkflowDefinition definition = new WorkflowDefinition("invalid-routing", List.of(
                new WorkflowNode.ManualTrigger("req-trigger", nullResult("trigger")),
                new WorkflowNode.Compose(
                        "req-object",
                        Map.of(),
                        new WorkflowResultDeclaration(
                                "object",
                                WorkflowResultDeclaration.WorkflowValueType.OBJECT,
                                new WorkflowRouting.Cases("", List.of(
                                        new WorkflowRouting.RouteCase("tooLarge", new WorkflowRouting.NumberComparison(
                                                WorkflowRouting.NumberOperator.GT,
                                                BigDecimal.TEN
                                        ))
                                ), "default")
                        ),
                        WorkflowFailurePolicy.failWorkflow()
                )
        ), List.of(edge("req-trigger", "default", "req-object")));

        assertThatThrownBy(() -> compiler.compile(definition))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Object and array routing require a subject pointer");
    }

    @Test
    void rejectsNodesThatCannotBeReachedFromTheManualTrigger() {
        WorkflowDefinition definition = new WorkflowDefinition("unreachable", List.of(
                new WorkflowNode.ManualTrigger("req-trigger", nullResult("trigger")),
                new WorkflowNode.Compose(
                        "req-unreachable",
                        Map.of(),
                        objectResult("unreachable"),
                        WorkflowFailurePolicy.failWorkflow()
                )
        ), List.of());

        assertThatThrownBy(() -> compiler.compile(definition))
                .isInstanceOf(WorkflowValidationException.class)
                .hasMessageContaining("Every node must be reachable");
    }

    private WorkflowNode.ToolCall tool(String requestId, String functionName, String resultName) {
        return new WorkflowNode.ToolCall(
                requestId,
                functionName,
                Map.of(),
                stringResult(resultName),
                WorkflowFailurePolicy.failWorkflow()
        );
    }

    private WorkflowResultDeclaration nullResult(String name) {
        return new WorkflowResultDeclaration(
                name,
                WorkflowResultDeclaration.WorkflowValueType.NULL,
                new WorkflowRouting.Cases("", List.of(), "default")
        );
    }

    private WorkflowResultDeclaration stringResult(String name) {
        return new WorkflowResultDeclaration(
                name,
                WorkflowResultDeclaration.WorkflowValueType.STRING,
                new WorkflowRouting.Cases("", List.of(), "default")
        );
    }

    private WorkflowResultDeclaration objectResult(String name) {
        return new WorkflowResultDeclaration(
                name,
                WorkflowResultDeclaration.WorkflowValueType.OBJECT,
                new WorkflowRouting.Cases("", List.of(), "default")
        );
    }

    private WorkflowEdge edge(String fromRequestId, String fromPort, String toRequestId) {
        return new WorkflowEdge(
                new WorkflowEndpoint(fromRequestId, fromPort),
                new WorkflowEndpoint(toRequestId, "in")
        );
    }
}
