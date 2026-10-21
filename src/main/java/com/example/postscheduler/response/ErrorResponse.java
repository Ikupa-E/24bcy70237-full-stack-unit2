package com.example.postscheduler.response;

import com.example.postscheduler.exception.ErrorCode;
import com.example.postscheduler.filter.CorrelationIdFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/** Standard structure for every error returned by the API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        int status,
        String error,
        ErrorCode errorCode,
        String message,
        String path,
        String correlationId,
        Map<String, String> errors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(HttpStatus status, ErrorCode code, String message,
                                   String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(false, status.value(), status.getReasonPhrase(), code, message,
                path, MDC.get(CorrelationIdFilter.MDC_KEY), fieldErrors, LocalDateTime.now());
    }
}
