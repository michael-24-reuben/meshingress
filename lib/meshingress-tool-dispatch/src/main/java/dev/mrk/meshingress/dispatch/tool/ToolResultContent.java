package dev.mrk.meshingress.dispatch.tool;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
public final class ToolResultContent extends StructuredContent {
    private String tool;
    private String operation;
    private String status;
    private boolean ok;
    private boolean mutating;
    private String receivedAt;
    private String message;
    private String exceptionType;
    private String baseUrl;
    private String upstreamRepository;
    private String upstreamCommit;
    private String upstreamModel;
    private String upstreamStatus;
    private String policy;
    private JsonNode request;
    private JsonNode response;
    private JsonNode wrapperResponse;
    private JsonNode commandResult;
    private JsonNode http;
    private JsonNode metadata;
    private JsonNode details;

    public ToolResultContent() {
        super(StructuredContentKind.Tool.TOOL_RESULT);
    }
}
