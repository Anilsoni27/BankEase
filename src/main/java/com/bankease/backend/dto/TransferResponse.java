package com.bankease.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransferResponse returns the receipt and outcome of a fund transfer attempt.
 */
public class TransferResponse {

    private Long transactionId;
    private String status;
    private String message;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private LocalDateTime timestamp;

    public TransferResponse() {
    }

    public TransferResponse(Long transactionId, String status, String message,
                            String fromAccountNumber, String toAccountNumber,
                            BigDecimal amount, BigDecimal remainingBalance,
                            LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.remainingBalance = remainingBalance;
        this.timestamp = timestamp;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

