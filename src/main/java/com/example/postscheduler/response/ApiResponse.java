package com.example.postscheduler.response;

import com.example.postscheduler.filter.CorrelationIdFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.LocalDateTime;

/** Standard envelope for every successful response. Errors use ErrorResponse. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
    private final LocalDateTime timestamp = LocalDateTime.now();

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
