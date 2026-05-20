package dev.mrk.meshingress.auth;

import org.springframework.http.HttpStatus;

public class InvalidMcpCredentialException extends RuntimeException {

    private final HttpStatus status;

    public InvalidMcpCredentialException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
