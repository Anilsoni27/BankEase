package com.bankease.backend.exception;

/**
 * Thrown when transfer parameters violate business rules
 * (e.g., self-transfer, invalid amount, account not owned by user).
 */
public class InvalidTransferException extends BankeaseException {

    public InvalidTransferException(String message) {
        super(message);
    }
}

