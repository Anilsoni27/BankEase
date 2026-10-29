package com.bankease.backend.service;

import com.bankease.backend.dto.TransactionResponse;
import com.bankease.backend.entity.Account;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.Transaction;
import com.bankease.backend.entity.User;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionService queries transaction history for customer accounts.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Feature 5: Retrieves transaction history for a given account number, sorted newest first.
     * Enforces ownership validation so customers can only inspect their own transactions.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactionHistory(String accountNumber, User currentUser) {
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " not found."));

        boolean isOwner = account.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Access denied: You do not own account " + accountNumber + ".");
        }

        // Fetch all transactions where this account is either the sender OR receiver, newest first
        List<Transaction> transactions = transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(account, account);

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper to map a Transaction entity into a TransactionResponse DTO.
     */
    public TransactionResponse mapToTransactionResponse(Transaction tx) {
        String fromAccNum = tx.getFromAccount() != null ? tx.getFromAccount().getAccountNumber() : "SYSTEM DEPOSIT";
        String fromOwner = tx.getFromAccount() != null ? tx.getFromAccount().getOwner().getFullName() : "BankEase System";

        String toAccNum = tx.getToAccount() != null ? tx.getToAccount().getAccountNumber() : "N/A";
        String toOwner = tx.getToAccount() != null ? tx.getToAccount().getOwner().getFullName() : "N/A";

        return new TransactionResponse(
                tx.getId(),
                fromAccNum,
                fromOwner,
                toAccNum,
                toOwner,
                tx.getAmount(),
                tx.getType().name(),
                tx.getStatus().name(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}

