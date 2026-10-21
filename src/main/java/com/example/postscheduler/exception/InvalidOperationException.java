package com.example.postscheduler.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an action is not allowed in the resource's current state. */
public class InvalidOperationException extends ApiException {
    public InvalidOperationException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.INVALID_OPERATION, message);
    }
}
