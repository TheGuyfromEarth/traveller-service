package com.travolish.traveller.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Returns structured 400 responses for Bean Validation failures
 * instead of letting them propagate as 500 Internal Server Error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                e -> e.getField(),
                e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value",
                (a, b) -> a
            ));
        return ResponseEntity.badRequest().body(Map.of(
            "status", 400,
            "error", "Validation failed",
            "fields", errors
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", 400,
            "error", "Constraint violation",
            "message", ex.getMessage()
        ));
    }

    /** Handles explicit null/blank checks thrown from service layer (e.g. hotel name required). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", 400,
            "error", "Invalid request",
            "message", ex.getMessage()
        ));
    }

    /** Returns 413 Payload Too Large instead of 500 when multipart upload exceeds the configured limit. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
            "status", 413,
            "error", "File too large",
            "message", "Maximum upload size is 200 MB. Please reduce the file size and try again."
        ));
    }
}
