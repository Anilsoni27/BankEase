package com.bankease.backend.entity;

/**
 * Role Enum defines access levels for BankEase users:
 * - CUSTOMER: Can view own account, make transfers, view own transaction history.
 * - ADMIN: Can view all users, all accounts, and system-wide transactions.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}

