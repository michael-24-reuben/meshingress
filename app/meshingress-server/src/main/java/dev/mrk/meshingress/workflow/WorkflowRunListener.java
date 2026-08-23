package dev.mrk.meshingress.workflow;

/** Observes one in-process workflow run without affecting its execution. */
public interface WorkflowRunListener {

    void onRunStarted(String runId);

    void onNodeStarted(WorkflowRun.NodeStarted nodeStarted);

    void onNodeCompleted(WorkflowRun.NodeResult nodeResult);

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
        public void onNodeStarted(WorkflowRun.NodeStarted nodeStarted) {
        }

        @Override
        public void onNodeCompleted(WorkflowRun.NodeResult nodeResult) {
        }

        @Override
        public void onRunCompleted(WorkflowRun run) {
        }
    }
}
