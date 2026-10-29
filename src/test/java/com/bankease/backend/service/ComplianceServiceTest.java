package com.bankease.backend.service;

import com.bankease.backend.entity.KycStatus;
import com.bankease.backend.entity.OtpVerification;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.exception.BankeaseException;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.OtpVerificationRepository;
import com.bankease.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ComplianceService complianceService;

    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        customer = new User("Rahul Sharma", "rahul@test.com", "pass", Role.CUSTOMER);
        customer.setId(10L);
        admin = new User("System Admin", "admin@test.com", "pass", Role.ADMIN);
        admin.setId(1L);
    }

    @Test
    @DisplayName("Should generate 6-digit OTP with simulated demo notice")
    void testGenerateOtp() {
        Map<String, Object> result = complianceService.generateOtp("rahul@test.com", "HIGH_VALUE_TRANSFER");

        assertNotNull(result);
        assertTrue(result.containsKey("otpCode"));
        String otpCode = (String) result.get("otpCode");
        assertEquals(6, otpCode.length());
        assertTrue(result.containsKey("notice"));
        assertTrue(((String) result.get("notice")).contains("SIMULATED OTP"));

        verify(otpVerificationRepository).save(any(OtpVerification.class));
        verify(auditLogService).log(eq("rahul@test.com"), eq("OTP_GENERATED"), anyString());
    }

    @Test
    @DisplayName("Should verify valid OTP successfully and mark as used")
    void testVerifyValidOtp() {
        OtpVerification otp = new OtpVerification("rahul@test.com", "654321", "HIGH_VALUE_TRANSFER", LocalDateTime.now().plusMinutes(5));
        when(otpVerificationRepository.findTopByUserEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc("rahul@test.com", "HIGH_VALUE_TRANSFER"))
                .thenReturn(Optional.of(otp));

        boolean valid = complianceService.verifyOtp("rahul@test.com", "654321", "HIGH_VALUE_TRANSFER");

        assertTrue(valid);
        assertTrue(otp.isUsed());
        verify(otpVerificationRepository).save(otp);
        verify(auditLogService).log(eq("rahul@test.com"), eq("OTP_VERIFIED"), anyString());
    }

    @Test
    @DisplayName("Should reject invalid OTP code and log failure")
    void testVerifyInvalidOtp() {
        OtpVerification otp = new OtpVerification("rahul@test.com", "654321", "HIGH_VALUE_TRANSFER", LocalDateTime.now().plusMinutes(5));
        when(otpVerificationRepository.findTopByUserEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc("rahul@test.com", "HIGH_VALUE_TRANSFER"))
                .thenReturn(Optional.of(otp));

        assertThrows(BankeaseException.class, () -> complianceService.verifyOtp("rahul@test.com", "000000", "HIGH_VALUE_TRANSFER"));
        verify(auditLogService).log(eq("rahul@test.com"), eq("OTP_FAILED"), anyString());
    }

    @Test
    @DisplayName("Should mask document numbers correctly to protect sensitive PII")
    void testMaskDocumentNumber() {
        assertEquals("XXXX-XXXX-9012", complianceService.maskDocumentNumber("1234-5678-9012"));
        assertEquals("XXXX-XXXX-9012", complianceService.maskDocumentNumber("123456789012"));
        assertEquals("XXXX-XXXX-234F", complianceService.maskDocumentNumber("ABCDE1234F"));
        assertEquals("XXXX-XXXX-XXXX", complianceService.maskDocumentNumber(""));
        assertEquals("XXXX-XXXX-XXXX", complianceService.maskDocumentNumber(null));
    }

    @Test
    @DisplayName("Should submit KYC with PENDING status and masked document in audit log")
    void testSubmitKycMasked() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = complianceService.submitKyc(customer, "Aadhaar Card", "1234-5678-9012");

        assertEquals(KycStatus.PENDING, updated.getKycStatus());
        verify(auditLogService).log(eq("rahul@test.com"), eq("KYC_SUBMITTED"), contains("XXXX-XXXX-9012"));
    }

    @Test
    @DisplayName("Should allow Admin to verify KYC to VERIFIED")
    void testAdminVerifyKyc() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User verified = complianceService.verifyKycStatus(10L, KycStatus.VERIFIED, admin);

        assertEquals(KycStatus.VERIFIED, verified.getKycStatus());
        verify(auditLogService).log(eq("admin@test.com"), eq("KYC_VERIFIED"), contains("rahul@test.com"));
    }

    @Test
    @DisplayName("Should reject KYC verification attempt by non-admin")
    void testCustomerCannotVerifyKyc() {
        assertThrows(UnauthorizedException.class, () -> complianceService.verifyKycStatus(10L, KycStatus.VERIFIED, customer));
    }

    @Test
    @DisplayName("Should update daily transfer limit with valid amount")
    void testUpdateDailyLimit() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = complianceService.updateDailyLimit(customer, new BigDecimal("75000.00"));

        assertEquals(new BigDecimal("75000.00"), updated.getDailyLimit());
        verify(auditLogService).log(eq("rahul@test.com"), eq("LIMIT_UPDATED"), contains("75000.00"));
    }

    @Test
    @DisplayName("Should reject zero or negative daily transfer limit")
    void testUpdateDailyLimitNegative() {
        assertThrows(BankeaseException.class, () -> complianceService.updateDailyLimit(customer, BigDecimal.ZERO));
        assertThrows(BankeaseException.class, () -> complianceService.updateDailyLimit(customer, new BigDecimal("-500")));
    }

    @Test
    @DisplayName("Should allow Admin to unlock a locked customer account")
    void testUnlockUser() {
        customer.setAccountLocked(true);
        customer.setFailedLoginAttempts(3);

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User unlocked = complianceService.unlockUser(10L, admin);

        assertFalse(unlocked.isAccountLocked());
        assertEquals(0, unlocked.getFailedLoginAttempts());
        verify(auditLogService).log(eq("admin@test.com"), eq("ACCOUNT_UNLOCKED"), contains("rahul@test.com"));
    }
}
