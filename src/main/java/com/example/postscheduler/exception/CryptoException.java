package com.example.postscheduler.exception;

/** Thrown when encryption or decryption fails. The message is never shown to API clients (they get a 500). */
public class CryptoException extends RuntimeException {
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
