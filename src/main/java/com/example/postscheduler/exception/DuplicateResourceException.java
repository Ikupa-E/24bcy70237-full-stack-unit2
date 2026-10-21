package com.example.postscheduler.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
