package com.bankease.backend.entity;

/**
 * TransactionType Enum indicates the nature of financial activity:
 * - TRANSFER: Funds moved from one account to another.
 * - DEPOSIT: Funds credited directly to an account (e.g., initial account opening demo credit).
 */
public enum TransactionType {
    TRANSFER,
    DEPOSIT
}

