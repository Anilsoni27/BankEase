package com.bankease.backend.entity;

/**
 * TransactionType Enum indicates the nature of financial activity:
 * - TRANSFER: Funds moved from one account to another.
 * - DEPOSIT: Funds credited directly to an account (e.g., initial account opening demo credit).
 * - BILL_PAY: Utility bill payment debited from account.
 * - LOAN_DISBURSEMENT: Approved loan credited directly to user account.
 * - INVESTMENT_DEBIT: Funds invested into FD/RD/Mutual Funds.
 * - INVESTMENT_CREDIT: Maturity or liquidation of FD/RD/MF credited back.
 */
public enum TransactionType {
    TRANSFER,
    DEPOSIT,
    BILL_PAY,
    LOAN_DISBURSEMENT,
    INVESTMENT_DEBIT,
    INVESTMENT_CREDIT
}
