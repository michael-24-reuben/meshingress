package dev.mrk.meshingress.repository.artifact;

public class RepositoryAccessDeniedException extends RuntimeException {
    public RepositoryAccessDeniedException(String message) {
        super(message);
    }
}
