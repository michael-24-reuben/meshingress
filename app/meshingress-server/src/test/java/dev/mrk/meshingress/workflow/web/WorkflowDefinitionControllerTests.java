package dev.mrk.meshingress.workflow.web;

import dev.mrk.meshingress.workflow.WorkflowDefinition;
import dev.mrk.meshingress.workflow.WorkflowRun;
import dev.mrk.meshingress.workflow.WorkflowRuntime;
import dev.mrk.meshingress.workflow.WorkflowInput;
import dev.mrk.meshingress.workflow.WorkflowNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowDefinitionControllerTests {

    private WorkflowRuntime runtime;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        runtime = mock(WorkflowRuntime.class);
        mvc = MockMvcBuilders.standaloneSetup(new WorkflowDefinitionController(runtime)).build();
    }

    @Test
    void runsOnlyTheTriggerReachableStudioNodes() throws Exception {
        when(runtime.run(any(), any(), any())).thenReturn(new WorkflowRun(
                "run_studio",
                WorkflowRun.Status.COMPLETED,
                Map.of("greeting", JsonNodeFactory.instance.objectNode().put("text", "Hello")),
                List.of(),
                null
        ));

        mvc.perform(post("/api/v1/workflows/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":"studio-live",
                                  "version":1,
                                  "nodes":[
                                    {"requestId":"r-001","kind":"trigger","arguments":{},"output":"trigger"},
                                    {"requestId":"r-002","kind":"tool","functionName":"powershell.cli.execute","arguments":{"script":"Set volume","arguments":["-NoProfile","-NonInteractive"]},"output":"volume"},
                                    {"requestId":"r-003","kind":"tool","functionName":"helloworld.greeting.greet","arguments":{"name":"From Studio input"},"output":"greeting"}
                                  ],
                                  "edges":[{"source":"r-001","target":"r-003"}]
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkflowDefinition> definition = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(runtime).run(definition.capture(), any(), any());
        assertThat(definition.getValue().nodes())
                .extracting(WorkflowNode::requestId)
                .containsExactly("r-001", "r-003");
        WorkflowNode.ToolCall greeting = (WorkflowNode.ToolCall) definition.getValue().nodes().get(1);
        assertThat(greeting.functionName())
                .isEqualTo("helloworld.greeting.greet");
        assertThat(((WorkflowInput.Literal) greeting.arguments().get("name")).value().asString())
                .isEqualTo("From Studio input");
    }
}
