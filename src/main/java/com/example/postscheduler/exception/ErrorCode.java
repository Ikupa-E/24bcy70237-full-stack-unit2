package com.example.postscheduler.exception;

/** Machine-readable error codes returned to API clients. */
public enum ErrorCode {
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    INVALID_PARAMETER,
    RESOURCE_NOT_FOUND,
    DUPLICATE_RESOURCE,
    INVALID_OPERATION,
    ENDPOINT_NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    UNAUTHORIZED,
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    FORBIDDEN,
    INTERNAL_ERROR
}
