package dev.mrk.meshingress.workflow.web;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.security.McpTransportContextFactory;
import dev.mrk.meshingress.security.McpTransportEvidence;
import dev.mrk.meshingress.workflow.WorkflowRun;
import dev.mrk.meshingress.workflow.WorkflowRuntime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/** Runs the graph currently shown in Studio without storing a workflow definition. */
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow execution", description = "Execute the current Studio graph without persisting it.")
public class WorkflowDefinitionController {

    private final WorkflowRuntime workflowRuntime;
    private final McpTransportContextFactory contextFactory;

    @Autowired
    public WorkflowDefinitionController(WorkflowRuntime workflowRuntime, McpTransportContextFactory contextFactory) {
        this.workflowRuntime = workflowRuntime;
        this.contextFactory = contextFactory;
    }

    /** @deprecated Test-only compatibility constructor; production uses the injected context factory. */
    @Deprecated
    public WorkflowDefinitionController(WorkflowRuntime workflowRuntime) {
        this(workflowRuntime, new McpTransportContextFactory(ignored -> dev.mrk.meshingress.api.McpPrincipal.anonymous()));
    }

    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Run the current Studio workflow graph")
    public ResponseEntity<WorkflowRun> run(
            @RequestBody StudioWorkflowDefinition definition,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        String effectiveSessionId = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
        String effectiveRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        McpCallContext context = contextFactory.create(
                new McpTransportEvidence(authorization, effectiveSessionId, effectiveRequestId, McpTransportEvidence.Transport.WORKFLOW), null);
        WorkflowRun run = workflowRuntime.run(definition.executableDefinition(), null, context);
        return ResponseEntity.ok()
                .header("Mcp-Session-Id", effectiveSessionId)
                .header("X-Request-Id", effectiveRequestId)
                .body(run);
    }
}
