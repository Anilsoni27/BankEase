package com.bankease.backend.exception;

/**
 * Thrown when an account does not have sufficient funds for a transfer.
 */
public class InsufficientBalanceException extends BankeaseException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}

