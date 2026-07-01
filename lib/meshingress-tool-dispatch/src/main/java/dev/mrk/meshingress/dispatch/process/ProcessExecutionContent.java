package dev.mrk.meshingress.dispatch.process;

import dev.mrk.meshingress.dispatch.StructuredContent;
import dev.mrk.meshingress.dispatch.StructuredContentKind;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class ProcessExecutionContent extends StructuredContent {
    @Setter
    private String status;
    @Setter
    private String command;
    private List<String> args = new ArrayList<>();
    @Setter
    private String workingDirectory;
    @Setter
    private Integer exitCode;
    @Setter
    private String stdout;
    @Setter
    private String stderr;
    @Setter
    private boolean timedOut;
    @Setter
    private Long durationMs;
    @Setter
    private String startedAt;
    @Setter
    private String finishedAt;
    @Setter
    private Integer trackCount;
    @Setter
    private JsonNode tracks;
    @Setter
    private String script;
    @Setter
    private String message;
    @Setter
    private JsonNode details;

    public ProcessExecutionContent() {
        super(StructuredContentKind.Process.PROCESS_EXECUTION);
    }

    public void setArgs(List<String> args) {
        this.args = args == null ? new ArrayList<>() : new ArrayList<>(args);
    }

}
