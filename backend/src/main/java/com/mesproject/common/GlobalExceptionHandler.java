package com.mesproject.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.name(),
                status.name(),
                ex.getReason() == null ? "Request failed" : ex.getReason(),
                req.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "VALIDATION_ERROR",
                "Invalid request payload",
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }
}

