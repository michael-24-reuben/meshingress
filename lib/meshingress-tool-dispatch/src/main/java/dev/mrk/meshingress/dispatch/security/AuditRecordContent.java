package dev.mrk.meshingress.dispatch.security;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class AuditRecordContent extends StructuredContent {
    private String id;
    private String timestamp;
    private String actor;
    private String action;
    private String target;
    private String outcome;
    private JsonNode details;

    public AuditRecordContent() {
        super(StructuredContentKind.Security.AUDIT_RECORD);
    }

}
