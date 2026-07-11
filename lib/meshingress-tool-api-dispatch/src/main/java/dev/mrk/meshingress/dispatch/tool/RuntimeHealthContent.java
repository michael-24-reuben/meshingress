package dev.mrk.meshingress.dispatch.tool;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class RuntimeHealthContent extends StructuredContent {
    @Setter
    private String status;
    @Setter
    private Long uptimeMs;
    private List<HealthCheckResult> checks = new ArrayList<>();

    public RuntimeHealthContent() {
        super(StructuredContentKind.Tool.RUNTIME_HEALTH);
    }

    public void setChecks(List<HealthCheckResult> checks) {
        this.checks = checks == null ? new ArrayList<>() : new ArrayList<>(checks);
    }
}
