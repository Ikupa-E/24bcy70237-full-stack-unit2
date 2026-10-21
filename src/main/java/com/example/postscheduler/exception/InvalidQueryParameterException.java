package com.example.postscheduler.exception;

import org.springframework.http.HttpStatus;

public class InvalidQueryParameterException extends ApiException {
    public InvalidQueryParameterException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, message);
    }
}
