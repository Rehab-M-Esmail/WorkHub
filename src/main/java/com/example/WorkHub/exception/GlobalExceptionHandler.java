package com.example.WorkHub.exception;

import com.example.WorkHub.models.ApiError;
import com.example.WorkHub.models.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiFieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ApiFieldError(err.getField(), err.getDefaultMessage()))
                .toList();

        ApiError error = new ApiError(
                Instant.now(), 400, "Validation Failed", "USR_001",
                "Request contains invalid fields",
                request.getRequestURI(),
                MDC.get("traceId"), details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ApiError error = new ApiError(
                Instant.now(), 404, "Not Found", "RES_001",
                ex.getMessage(),
                request.getRequestURI(),
                MDC.get("traceId"), null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiError error = new ApiError(
                Instant.now(), 500, "Internal Server Error", "SYS_001",
                "An unexpected error occurred",
                request.getRequestURI(),
                MDC.get("traceId"), null
        );
        return ResponseEntity.internalServerError().body(error);
    }
}