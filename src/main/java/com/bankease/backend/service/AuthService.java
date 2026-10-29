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
 * AuthService handles user registration, JWT generation, and login security controls.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final TransactionRecordService transactionRecordService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;

    private static final BigDecimal INITIAL_DEMO_BALANCE = new BigDecimal("10000.00");
    private static final int MAX_FAILED_ATTEMPTS = 3;

    public AuthService(UserRepository userRepository,
                       AccountService accountService,
                       TransactionRecordService transactionRecordService,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountService = accountService;
        this.transactionRecordService = transactionRecordService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
    }

    /**
     * Feature 1: Register with JWT & Welcome Bonus
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BankeaseException("An account with email " + cleanEmail + " already exists.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword().trim());

        User user = new User(
                request.getFullName().trim(),
                cleanEmail,
                encodedPassword,
                Role.CUSTOMER
        );
        user.setKycStatus(KycStatus.PENDING);
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

        auditLogService.log(cleanEmail, "REGISTRATION_SUCCESS", "New customer account registered with welcome bonus.");

        // Generate JWT token
        String token = tokenService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
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
     * Feature 2: Login with JWT & Account Lockout Compliance (3 Failed Attempts)
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        // Check if account is locked
        if (user.isAccountLocked()) {
            auditLogService.log(cleanEmail, "LOGIN_BLOCKED", "Attempt to sign into locked account.");
            throw new UnauthorizedException("Account is locked due to 3 consecutive failed login attempts. Please contact Administrator to unlock.");
        }

        // Verify password against BCrypt hash
        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                userRepository.save(user);
                auditLogService.log(cleanEmail, "ACCOUNT_LOCKED", "Account locked after 3 consecutive failed login attempts.");
                throw new UnauthorizedException("Account has been LOCKED due to 3 consecutive failed login attempts. Contact Administrator.");
            } else {
                userRepository.save(user);
                int remaining = MAX_FAILED_ATTEMPTS - attempts;
                auditLogService.log(cleanEmail, "LOGIN_FAILED", "Invalid password attempt " + attempts + "/" + MAX_FAILED_ATTEMPTS);
                throw new UnauthorizedException("Invalid email or password. " + remaining + " attempts remaining before account lockout.");
            }
        }

        // Reset failed login counter on success
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        auditLogService.log(cleanEmail, "LOGIN_SUCCESS", "User authenticated successfully via JWT.");

        // Issue JWT token
        String token = tokenService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
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
     * Logout
     */
    public void logout(String authHeader) {
        if (authHeader != null) {
            String token = authHeader.trim().replace("Bearer ", "").trim();
            tokenService.invalidateToken(token);
        }
    }
}
