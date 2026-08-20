package dev.mrk.meshingress.workflow;

/** Observes one in-process workflow run without affecting its execution. */
public interface WorkflowRunListener {

    void onRunStarted(String runId);

    void onNodeStarted(String requestId);

    void onNodeCompleted(WorkflowRun.NodeOutcome outcome);

    void onRunCompleted(WorkflowRun run);

    static WorkflowRunListener noop() {
        return NoopWorkflowRunListener.INSTANCE;
    }

    enum NoopWorkflowRunListener implements WorkflowRunListener {
        INSTANCE;

        @Override
        public void onRunStarted(String runId) {
        }

        @Override
        public void onNodeStarted(String requestId) {
        }

        @Override
        public void onNodeCompleted(WorkflowRun.NodeOutcome outcome) {
        }

        @Override
        public void onRunCompleted(WorkflowRun run) {
        }
    }
}
