package dev.mrk.meshingress.dispatch.security;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class SecurityDecisionContent extends StructuredContent {
    @Setter
    private String decision;
    @Setter
    private String reason;
    private List<String> scopes = new ArrayList<>();
    @Setter
    private String riskLevel;
    @Setter
    private boolean requiresApproval;

    public SecurityDecisionContent() {
        super(StructuredContentKind.Security.SECURITY_DECISION);
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
    }

}
