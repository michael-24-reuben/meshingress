package dev.mrk.toolspace.transform;

import dev.mrk.meshingress.api.McpCallContext;
import dev.mrk.meshingress.api.result.DispatchExecutionResult;
import dev.mrk.meshingress.api.tools.annotation.McpConfigureMapping;
import dev.mrk.meshingress.api.tools.annotation.McpFunction;
import dev.mrk.meshingress.api.tools.annotation.McpTool;
import dev.mrk.meshingress.api.tools.annotation.McpToolMapping;
import dev.mrk.meshingress.api.tools.annotation.McpToolScopes;
import dev.mrk.meshingress.dispatch.data.RecordsContent;
import dev.mrk.meshingress.dispatch.text.CodeContent;
import dev.mrk.meshingress.scopes.McpToolScope;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@McpTool(value = "code-block.transform", title = "Transform", description = "Backend-only polyglot transformations from ritz078/transform.")
@McpToolScopes(McpToolScope.PROCESS_EXECUTE)
@McpToolMapping("tools")
public final class TransformTool {
    private final ObjectMapper objectMapper;
    private final TransformBackend backend;

    TransformTool(ObjectMapper objectMapper, TransformBackend backend) { this.objectMapper = objectMapper; this.backend = backend; }

    @McpConfigureMapping(timeoutMs = 120_000)
    @McpFunction(value = "start", title = "Transform source", description = "Dispatch one selected Transform conversion. This is the only conversion endpoint.")
    public DispatchExecutionResult transform(TransformArgs arguments, McpCallContext context) {
        if (arguments == null || arguments.type() == null || arguments.value() == null) {
            return DispatchExecutionResult.builder().error("INVALID_ARGUMENTS", "type and value are required.").status("failed").build();
        }
        try {
            String result = backend.transform(arguments);
            CodeContent structured = new CodeContent(result, arguments.type().outputLanguage());
            structured.setTitle(arguments.type().id());
            return DispatchExecutionResult.builder().text(result).structuredContent(structured)
                    .status("completed").summary("Completed " + arguments.type().id() + ".").build();
        } catch (Exception exception) {
            return DispatchExecutionResult.builder().error("TRANSFORM_FAILED", exception.getMessage()).status("failed").summary("Transform conversion failed.").build();
        }
    }

    @McpFunction(value = "list-types", title = "List transform types", description = "List every stable transform type accepted by transform.")
    public DispatchExecutionResult listTypes(McpCallContext context) {
        List<TransformListItem> types = Arrays.stream(TransformType.values()).map(TransformListItem::from).toList();
        RecordsContent structured = new RecordsContent();
        structured.setRecords(types.stream().map(objectMapper::<JsonNode>valueToTree).toList());
        structured.setTotal(types.size());
        return DispatchExecutionResult.builder().array(objectMapper.valueToTree(types)).structuredContent(structured)
                .status("completed").summary(types.size() + " transform types available.").build();
    }
}
