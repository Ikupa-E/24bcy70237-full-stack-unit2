package com.example.postscheduler.exception;

import org.springframework.http.HttpStatus;

/** A refresh token is unknown, expired, revoked or has been reused. */
public class InvalidTokenException extends ApiException {
    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN, message);
    }
}
