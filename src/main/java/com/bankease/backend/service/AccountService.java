package com.bankease.backend.service;

import com.bankease.backend.dto.AccountResponse;
import com.bankease.backend.entity.Account;
import com.bankease.backend.entity.AccountType;
import com.bankease.backend.entity.User;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AccountService handles bank account creation, number generation, and balance inquiries.
 *
 * Annotations explained:
 * - @Service: Tells Spring to manage this class as a business service component.
 * - @Transactional(readOnly = true): Optimizes database performance for read-only query operations.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Creates a new bank account with the given initial balance and account type.
     */
    @Transactional
    public Account createAccount(User user, BigDecimal initialBalance, AccountType accountType) {
        String accountNumber = generateUniqueAccountNumber();
        Account account = new Account(accountNumber, user, initialBalance, accountType);
        return accountRepository.save(account);
    }

    /**
     * Generates a unique 10-digit account number starting with "100".
     * Checks database to prevent collision.
     */
    public String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            // Generates a 7-digit number (1000000 to 9999999) prefixed by "100" -> 10 digits
            int randomSuffix = 1000000 + secureRandom.nextInt(9000000);
            accountNumber = "100" + randomSuffix;
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    /**
     * Retrieves all accounts owned by a specific user.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(User user) {
        return accountRepository.findByOwner(user)
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds an account by account number or throws ResourceNotFoundException.
     */
    @Transactional(readOnly = true)
    public Account getAccountEntityByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account with number " + accountNumber + " not found."));
    }

    /**
     * Finds an account by number and returns DTO representation.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountResponseByNumber(String accountNumber) {
        Account account = getAccountEntityByNumber(accountNumber);
        return mapToAccountResponse(account);
    }

    /**
     * Helper method to map Account JPA entity to AccountResponse DTO.
     */
    public AccountResponse mapToAccountResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getOwner().getFullName(),
                account.getOwner().getEmail(),
                account.getCreatedAt()
        );
    }
}

