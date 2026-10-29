package com.bankease.backend.exception;

/**
 * Thrown when an unauthenticated request attempts to access protected endpoints,
 * or when token is missing, invalid, or expired.
 */
public class UnauthorizedException extends BankeaseException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

