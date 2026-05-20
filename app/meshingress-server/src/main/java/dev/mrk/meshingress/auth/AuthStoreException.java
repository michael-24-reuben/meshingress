package dev.mrk.meshingress.auth;

import org.springframework.http.HttpStatus;

public class AuthStoreException extends RuntimeException {

    private final HttpStatus status;

    public AuthStoreException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
