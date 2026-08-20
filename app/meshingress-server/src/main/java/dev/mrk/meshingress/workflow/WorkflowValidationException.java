package dev.mrk.meshingress.workflow;

/** Raised when a workflow definition or runtime result violates the beta contract. */
public class WorkflowValidationException extends RuntimeException {
    public WorkflowValidationException(String message) {
        super(message);
    }
}
