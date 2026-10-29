package com.bankease.backend.service;

import com.bankease.backend.dto.TransferRequest;
import com.bankease.backend.dto.TransferResponse;
import com.bankease.backend.entity.*;
import com.bankease.backend.exception.InsufficientBalanceException;
import com.bankease.backend.exception.InvalidTransferException;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransferService coordinates atomic fund transfers between bank accounts.
 *
 * ============================================================================
 * EXPLANATION OF @Transactional LOGIC AND ACID PROPERTIES
 * ============================================================================
 * 1. Atomicity: In a bank transfer, two balance changes occur:
 *      Step A: Debit money from Sender account.
 *      Step B: Credit money into Receiver account.
 *    If the server crashes or an unexpected error occurs during Step B,
 *    Spring rolls back Step A completely so money never disappears into thin air.
 *
 * 2. Consistency: Database constraints (like NOT NULL, UNIQUE, and foreign keys)
 *    are enforced. The sum of balances remains balanced.
 *
 * 3. Isolation: Concurrent transfers cannot view partial states or dirty reads.
 *
 * 4. Durability: Once committed to MySQL, the new balances are permanent.
 *
 * 5. Audit Logging with Propagation.REQUIRES_NEW:
 *    When a transfer fails validation (e.g., Insufficient Balance), we record
 *    a FAILED Transaction record via TransactionRecordService, which runs in its
 *    own independent transaction so the audit trail is never rolled back!
 * ============================================================================
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRecordService transactionRecordService;

    public TransferService(AccountRepository accountRepository,
                           TransactionRecordService transactionRecordService) {
        this.accountRepository = accountRepository;
        this.transactionRecordService = transactionRecordService;
    }

    /**
     * Executes a fund transfer from one account to another.
     *
     * @param currentUser Authenticated user initiating the transfer
     * @param request     Transfer parameters (source, destination, amount, remarks)
     * @return TransferResponse with receipt details
     */
    @Transactional
    public TransferResponse transferFunds(User currentUser, TransferRequest request) {
        BigDecimal amount = request.getAmount();
        String fromAccNum = request.getFromAccountNumber().trim();
        String toAccNum = request.getToAccountNumber().trim();
        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Fund transfer from " + fromAccNum + " to " + toAccNum;

        // --------------------------------------------------------------------
        // Validation 1: Amount must be greater than zero
        // --------------------------------------------------------------------
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly greater than zero.");
        }

        // --------------------------------------------------------------------
        // Validation 2: Source account must exist
        // --------------------------------------------------------------------
        Account fromAccount = accountRepository.findByAccountNumber(fromAccNum)
                .orElseThrow(() -> new ResourceNotFoundException("Source account " + fromAccNum + " does not exist."));

        // --------------------------------------------------------------------
        // Validation 3: Current user must be the owner of the source account
        // (unless current user is an ADMIN)
        // --------------------------------------------------------------------
        boolean isOwner = fromAccount.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new InvalidTransferException("Unauthorized: You do not own the source account " + fromAccNum + ".");
        }

        // --------------------------------------------------------------------
        // Validation 4: Cannot transfer to the same account
        // --------------------------------------------------------------------
        if (fromAccNum.equalsIgnoreCase(toAccNum)) {
            // Save a FAILED transaction record for audit compliance
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

        // --------------------------------------------------------------------
        // Validation 5: Destination account must exist
        // --------------------------------------------------------------------
        Account toAccount = accountRepository.findByAccountNumber(toAccNum)
                .orElse(null);

        if (toAccount == null) {
            // Save a FAILED transaction record
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

        // --------------------------------------------------------------------
        // Validation 6: Sender must have sufficient balance
        // --------------------------------------------------------------------
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            // Save a FAILED transaction record
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
        // 1. Debit sender
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // 2. Credit receiver
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        // 3. Record successful transaction
        Transaction successTx = transactionRecordService.record(
                fromAccount,
                toAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.SUCCESS,
                description
        );

        // 4. Return receipt DTO
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

