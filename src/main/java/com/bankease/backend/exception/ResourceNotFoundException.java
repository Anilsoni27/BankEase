package com.bankease.backend.exception;

/**
 * Thrown when an entity (User, Account, Transaction) cannot be found in the database.
 */
public class ResourceNotFoundException extends BankeaseException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

