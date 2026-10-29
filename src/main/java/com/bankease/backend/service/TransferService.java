package com.bankease.backend.service;

import com.bankease.backend.dto.TransferRequest;
import com.bankease.backend.dto.TransferResponse;
import com.bankease.backend.entity.*;
import com.bankease.backend.exception.BankeaseException;
import com.bankease.backend.exception.InsufficientBalanceException;
import com.bankease.backend.exception.InvalidTransferException;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransferService coordinates atomic fund transfers between bank accounts
 * with compliance limits and OTP security checks.
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRecordService transactionRecordService;
    private final ComplianceService complianceService;
    private final AuditLogService auditLogService;

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("25000.00");

    public TransferService(AccountRepository accountRepository,
                           TransactionRecordService transactionRecordService,
                           ComplianceService complianceService,
                           AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.transactionRecordService = transactionRecordService;
        this.complianceService = complianceService;
        this.auditLogService = auditLogService;
    }

    /**
     * Executes an atomic fund transfer.
     */
    @Transactional
    public TransferResponse transferFunds(User currentUser, TransferRequest request) {
        BigDecimal amount = request.getAmount();
        String fromAccNum = request.getFromAccountNumber().trim();
        String toAccNum = request.getToAccountNumber().trim();
        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Fund transfer from " + fromAccNum + " to " + toAccNum;

        // 1. Amount > 0 check
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly greater than zero.");
        }

        // 2. Source account check
        Account fromAccount = accountRepository.findByAccountNumber(fromAccNum)
                .orElseThrow(() -> new ResourceNotFoundException("Source account " + fromAccNum + " does not exist."));

        // 3. Ownership check
        boolean isOwner = fromAccount.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new InvalidTransferException("Unauthorized: You do not own the source account " + fromAccNum + ".");
        }

        // 4. Same account check
        if (fromAccNum.equalsIgnoreCase(toAccNum)) {
            transactionRecordService.record(
                    fromAccount,
                    fromAccount,
                    amount,
                    TransactionType.TRANSFER,
                    TransactionStatus.FAILED,
                    "Self-transfer rejected: Source and destination accounts are identical"
            );
            throw new InvalidTransferException("Cannot transfer funds to the same account.");
        }

        // 5. Destination account check
        Account toAccount = accountRepository.findByAccountNumber(toAccNum)
                .orElse(null);

        if (toAccount == null) {
            transactionRecordService.record(
                    fromAccount,
                    null,
                    amount,
                    TransactionType.TRANSFER,
                    TransactionStatus.FAILED,
                    "Transfer failed: Destination account " + toAccNum + " does not exist"
            );
            throw new ResourceNotFoundException("Destination account " + toAccNum + " was not found.");
        }

        // 6. Daily limit compliance check
        if (currentUser.getDailyLimit() != null && amount.compareTo(currentUser.getDailyLimit()) > 0) {
            transactionRecordService.record(
                    fromAccount,
                    toAccount,
                    amount,
                    TransactionType.TRANSFER,
                    TransactionStatus.FAILED,
                    "Compliance rejection: Amount ₹" + amount + " exceeds daily limit of ₹" + currentUser.getDailyLimit()
            );
            auditLogService.log(currentUser.getEmail(), "TRANSFER_LIMIT_EXCEEDED", "Attempted transfer ₹" + amount + " above limit ₹" + currentUser.getDailyLimit());
            throw new BankeaseException("Transfer exceeds your daily transfer limit of ₹" + currentUser.getDailyLimit() + ". Adjust your limit in Compliance Settings.");
        }

        // 7. High-Value Transfer OTP Compliance (Threshold: ₹ 25,000)
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            if (request.getOtpCode() == null || request.getOtpCode().trim().isEmpty()) {
                throw new BankeaseException("High-value transfer of ₹" + amount + " requires 2-factor OTP authorization. Please generate and provide an OTP.");
            }
            complianceService.verifyOtp(currentUser.getEmail(), request.getOtpCode().trim(), "HIGH_VALUE_TRANSFER");
        }

        // 8. Balance check
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            transactionRecordService.record(
                    fromAccount,
                    toAccount,
                    amount,
                    TransactionType.TRANSFER,
                    TransactionStatus.FAILED,
                    "Transfer failed: Insufficient balance. Available: ₹" + fromAccount.getBalance() + ", Requested: ₹" + amount
            );
            throw new InsufficientBalanceException("Insufficient funds. Available balance: ₹" + fromAccount.getBalance());
        }

        // --------------------------------------------------------------------
        // ATOMIC EXECUTION: Balance mutation
        // --------------------------------------------------------------------
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        Transaction successTx = transactionRecordService.record(
                fromAccount,
                toAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.SUCCESS,
                description
        );

        auditLogService.log(currentUser.getEmail(), "TRANSFER_SUCCESS", "Transferred ₹" + amount + " from " + fromAccNum + " to " + toAccNum);

        return new TransferResponse(
                successTx.getId(),
                TransactionStatus.SUCCESS.name(),
                "Transfer completed successfully.",
                fromAccNum,
                toAccNum,
                amount,
                fromAccount.getBalance(),
                LocalDateTime.now()
        );
    }
}
