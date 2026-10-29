package com.bankease.backend.entity;

/**
 * TransactionStatus Enum records whether an operation succeeded or failed:
 * - SUCCESS: Transfer or deposit completed and account balances were updated.
 * - FAILED: Transaction failed validation (e.g., insufficient balance, invalid receiver).
 */
public enum TransactionStatus {
    SUCCESS,
    FAILED
}

