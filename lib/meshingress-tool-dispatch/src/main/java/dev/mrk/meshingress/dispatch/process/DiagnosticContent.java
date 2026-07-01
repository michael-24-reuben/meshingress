package dev.mrk.meshingress.dispatch.process;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class DiagnosticContent extends StructuredContent {
    @Setter
    private String status;
    @Setter
    private String summary;
    private List<DiagnosticIssue> issues = new ArrayList<>();

    public DiagnosticContent() {
        super(StructuredContentKind.Process.PROCESS_DIAGNOSTIC);
    }

    public void setIssues(List<DiagnosticIssue> issues) {
        this.issues = issues == null ? new ArrayList<>() : new ArrayList<>(issues);
    }
}
