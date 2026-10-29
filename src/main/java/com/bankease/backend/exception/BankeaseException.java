package com.bankease.backend.exception;

/**
 * Base unchecked exception for BankEase business logic errors.
 */
public class BankeaseException extends RuntimeException {

    public BankeaseException(String message) {
        super(message);
    }

    public BankeaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

