package com.bankease.backend.service;

import com.bankease.backend.dto.AccountResponse;
import com.bankease.backend.dto.AuthResponse;
import com.bankease.backend.dto.LoginRequest;
import com.bankease.backend.dto.RegisterRequest;
import com.bankease.backend.entity.*;
import com.bankease.backend.exception.BankeaseException;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.UserRepository;
import com.bankease.backend.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * AuthService handles user registration, password verification, and session token generation.
 *
 * Annotations explained:
 * - @Service: Declares this class as a Spring Service bean.
 * - @Transactional: Ensures operations like creating a user, creating an account,
 *   and recording the initial deposit succeed together or roll back entirely.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final TransactionRecordService transactionRecordService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Initial demo funds gifted to every newly registered customer
    private static final BigDecimal INITIAL_DEMO_BALANCE = new BigDecimal("10000.00");

    public AuthService(UserRepository userRepository,
                       AccountService accountService,
                       TransactionRecordService transactionRecordService,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.transactionRecordService = transactionRecordService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Feature 1: Register
     * 1. Validates that email is unique.
     * 2. Hashes user password with BCrypt (salted, 10 rounds).
     * 3. Creates User record with role CUSTOMER.
     * 4. Automatically opens a SAVINGS account with ₹ 10,000.00 demo balance.
     * 5. Saves initial DEPOSIT transaction record.
     * 6. Issues an in-memory session token (random UUID).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BankeaseException("An account with email " + cleanEmail + " already exists.");
        }

        // Hash the password securely with BCrypt
        String encodedPassword = passwordEncoder.encode(request.getPassword().trim());

        // Create and save User
        User user = new User(
                request.getFullName().trim(),
                cleanEmail,
                encodedPassword,
                Role.CUSTOMER
        );
        User savedUser = userRepository.save(user);

        // Open default SAVINGS account with initial balance of 10000
        Account account = accountService.createAccount(savedUser, INITIAL_DEMO_BALANCE, AccountType.SAVINGS);

        // Record initial demo credit deposit transaction
        transactionRecordService.record(
                null,
                account,
                INITIAL_DEMO_BALANCE,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Initial demo credit on account opening"
        );

        // Generate session token and assemble response
        String token = tokenService.generateToken(savedUser.getId());
        AccountResponse accountResponse = accountService.mapToAccountResponse(account);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                Collections.singletonList(accountResponse)
        );
    }

    /**
     * Feature 2: Login
     * 1. Finds user by email.
     * 2. Compares plain password with stored BCrypt hash using passwordEncoder.matches().
     * 3. Generates and stores a random UUID session token in memory.
     * 4. Returns token along with user and account info.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        // Verify password against BCrypt hash
        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        // Generate session token
        String token = tokenService.generateToken(user.getId());

        // Fetch user's active accounts
        List<AccountResponse> accounts = accountService.getAccountsByUser(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                accounts
        );
    }

    /**
     * Logout invalidates the active session token in memory.
     */
    public void logout(String authHeader) {
        if (authHeader != null) {
            String token = authHeader.trim();
            if (token.toLowerCase().startsWith("bearer ")) {
                token = token.substring(7).trim();
            }
            tokenService.invalidateToken(token);
        }
    }
}

