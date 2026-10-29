package com.bankease.backend.service;

import com.bankease.backend.dto.AccountResponse;
import com.bankease.backend.dto.TransactionResponse;
import com.bankease.backend.dto.UserResponse;
import com.bankease.backend.entity.Account;
import com.bankease.backend.entity.User;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.TransactionRepository;
import com.bankease.backend.repository.UserRepository;
import com.bankease.backend.security.AuthHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminService exposes reporting and administrative queries for ADMIN users.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthHelper authHelper;

    public AdminService(UserRepository userRepository,
                        AccountRepository accountRepository,
                        TransactionRepository transactionRepository,
                        AccountService accountService,
                        TransactionService transactionService,
                        AuthHelper authHelper) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.authHelper = authHelper;
    }

    /**
     * Lists all registered users and their associated accounts.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(User adminUser) {
        authHelper.requireAdmin(adminUser);

        List<User> users = userRepository.findAll();
        return users.stream().map(user -> {
            List<Account> accounts = accountRepository.findByOwner(user);
            List<AccountResponse> accountResponses = accounts.stream()
                    .map(accountService::mapToAccountResponse)
                    .collect(Collectors.toList());

            return new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getKycStatus() != null ? user.getKycStatus().name() : "PENDING",
                    user.isAccountLocked(),
                    user.getDailyLimit(),
                    user.getCreatedAt(),
                    accountResponses
            );
        }).collect(Collectors.toList());
    }

    /**
     * Lists all bank accounts in the system.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts(User adminUser) {
        authHelper.requireAdmin(adminUser);
        return accountRepository.findAll()
                .stream()
                .map(accountService::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lists all financial transactions across all accounts, newest first.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions(User adminUser) {
        authHelper.requireAdmin(adminUser);
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(transactionService::mapToTransactionResponse)
                .collect(Collectors.toList());
    }
}

