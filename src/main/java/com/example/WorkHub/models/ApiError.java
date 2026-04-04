package com.example.WorkHub.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // Omit null fields from JSON
public class ApiError {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String traceId;
    private final String userAgent;
    private final List<ApiFieldError> details;

    public ApiError(
                                Instant timestamp, int status, String error, String userAgent,
                                String message, String path, String traceId, List<ApiFieldError> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.userAgent = userAgent;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.details = details;
    }
}
