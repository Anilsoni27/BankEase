package com.bankease.backend.service;

import com.bankease.backend.dto.AccountResponse;
import com.bankease.backend.dto.AuthResponse;
import com.bankease.backend.dto.LoginRequest;
import com.bankease.backend.dto.RegisterRequest;
import com.bankease.backend.entity.*;
import com.bankease.backend.exception.BankeaseException;
import com.bankease.backend.repository.UserRepository;
import com.bankease.backend.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionRecordService transactionRecordService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Rahul Sharma", "rahul@bankease.com", "$2a$10$hashedPassword", Role.CUSTOMER);
        sampleUser.setId(2L);
        sampleAccount = new Account("1001001001", sampleUser, new BigDecimal("10000.00"), AccountType.SAVINGS);
        sampleAccount.setId(2L);
    }

    @Test
    @DisplayName("Registration should set KYC to PENDING and open SAVINGS account with ₹10,000")
    void testRegisterUserDefaultKycPending() {
        RegisterRequest req = new RegisterRequest("Amit Verma", "amit@bankease.com", "Password@123");

        when(userRepository.existsByEmail("amit@bankease.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        Account newAcc = new Account("1005839201", sampleUser, new BigDecimal("10000.00"), AccountType.SAVINGS);
        when(accountService.createAccount(any(User.class), eq(new BigDecimal("10000.00")), eq(AccountType.SAVINGS)))
                .thenReturn(newAcc);
        when(accountService.mapToAccountResponse(newAcc)).thenReturn(
                new AccountResponse(5L, "1005839201", AccountType.SAVINGS.name(), new BigDecimal("10000.00"), "Amit Verma", "amit@bankease.com", LocalDateTime.now())
        );
        when(tokenService.generateToken(anyLong(), anyString(), anyString())).thenReturn("mock-jwt-token");

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("mock-jwt-token", resp.getToken());
        assertEquals("Amit Verma", resp.getFullName());
        assertEquals("amit@bankease.com", resp.getEmail());

        // Verify user saved with PENDING KYC
        verify(userRepository).save(argThat(user -> user.getKycStatus() == KycStatus.PENDING));
        verify(accountService).createAccount(any(User.class), eq(new BigDecimal("10000.00")), eq(AccountType.SAVINGS));
        verify(transactionRecordService).record(isNull(), eq(newAcc), eq(new BigDecimal("10000.00")), eq(TransactionType.DEPOSIT), eq(TransactionStatus.SUCCESS), anyString());
    }

    @Test
    @DisplayName("Login with valid credentials should succeed and return JWT token")
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest("rahul@bankease.com", "rahul123");
        sampleUser.setFailedLoginAttempts(1); // Pre-set to 1 to test reset counter

        when(userRepository.findByEmail("rahul@bankease.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("rahul123", "$2a$10$hashedPassword")).thenReturn(true);
        when(tokenService.generateToken(2L, "rahul@bankease.com", "CUSTOMER")).thenReturn("valid-jwt-token");
        when(accountService.getAccountsByUser(sampleUser)).thenReturn(Collections.singletonList(
                new AccountResponse(2L, "1001001001", AccountType.SAVINGS.name(), new BigDecimal("10000.00"), "Rahul Sharma", "rahul@bankease.com", LocalDateTime.now())
        ));

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("valid-jwt-token", resp.getToken());
        assertEquals(0, sampleUser.getFailedLoginAttempts());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("3 consecutive failed logins should trigger automatic account lockout")
    void testThreeStrikesAccountLockout() {
        LoginRequest req = new LoginRequest("rahul@bankease.com", "wrongPassword");

        when(userRepository.findByEmail("rahul@bankease.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        // Strike 1
        assertThrows(BankeaseException.class, () -> authService.login(req));
        assertEquals(1, sampleUser.getFailedLoginAttempts());
        assertFalse(sampleUser.isAccountLocked());

        // Strike 2
        assertThrows(BankeaseException.class, () -> authService.login(req));
        assertEquals(2, sampleUser.getFailedLoginAttempts());
        assertFalse(sampleUser.isAccountLocked());

        // Strike 3 - triggers lockout
        BankeaseException ex = assertThrows(BankeaseException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().toLowerCase().contains("locked"));
        assertEquals(3, sampleUser.getFailedLoginAttempts());
        assertTrue(sampleUser.isAccountLocked());

        verify(auditLogService).log(eq("rahul@bankease.com"), eq("ACCOUNT_LOCKED"), anyString());
    }
}
