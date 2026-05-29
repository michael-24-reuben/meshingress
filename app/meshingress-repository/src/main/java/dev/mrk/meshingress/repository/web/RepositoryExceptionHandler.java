package dev.mrk.meshingress.repository.web;

import dev.mrk.meshingress.repository.artifact.RepositoryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RepositoryExceptionHandler {

    @ExceptionHandler(RepositoryException.class)
    ProblemDetail handleRepositoryException(RepositoryException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Repository request failed");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
