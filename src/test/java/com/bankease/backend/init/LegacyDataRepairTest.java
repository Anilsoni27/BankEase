package com.bankease.backend.init;

import com.bankease.backend.entity.KycStatus;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.repository.UserRepository;
import com.bankease.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyDataRepairTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private LegacyDataRepair legacyDataRepair;
    private final BigDecimal defaultDailyLimit = new BigDecimal("100000.00");

    @BeforeEach
    void setUp() {
        legacyDataRepair = new LegacyDataRepair(userRepository, auditLogService, defaultDailyLimit);
    }

    @Test
    @DisplayName("Should repair user with dailyLimit = 0 to default ₹100,000 and write audit log")
    void testRepairUserWithZeroLimit() {
        User legacyUser = new User("Rahul Sharma", "rahul@bankease.com", "pass", Role.CUSTOMER);
        legacyUser.setId(2L);
        legacyUser.setDailyLimit(BigDecimal.ZERO);

        when(userRepository.findAll()).thenReturn(Collections.singletonList(legacyUser));

        int repairedCount = legacyDataRepair.repairLegacyData();

        assertEquals(1, repairedCount);
        assertEquals(defaultDailyLimit, legacyUser.getDailyLimit());
        verify(userRepository).save(legacyUser);
        verify(auditLogService).log(eq("rahul@bankease.com"), eq("LEGACY_DATA_REPAIRED"), contains("dailyLimit=100000.00"));
    }

    @Test
    @DisplayName("Should repair user with dailyLimit = null to default ₹100,000")
    void testRepairUserWithNullLimit() {
        User legacyUser = new User("Priya Patel", "priya@bankease.com", "pass", Role.CUSTOMER);
        legacyUser.setId(3L);
        legacyUser.setDailyLimit(null);

        when(userRepository.findAll()).thenReturn(Collections.singletonList(legacyUser));

        int repairedCount = legacyDataRepair.repairLegacyData();

        assertEquals(1, repairedCount);
        assertEquals(defaultDailyLimit, legacyUser.getDailyLimit());
        verify(userRepository).save(legacyUser);
        verify(auditLogService).log(eq("priya@bankease.com"), eq("LEGACY_DATA_REPAIRED"), contains("dailyLimit=100000.00"));
    }

    @Test
    @DisplayName("Should not modify user who already has a valid daily limit (e.g. ₹50,000)")
    void testDoNotModifyUserWithValidLimit() {
        User validUser = new User("Amit Verma", "amit@bankease.com", "pass", Role.CUSTOMER);
        validUser.setId(4L);
        validUser.setDailyLimit(new BigDecimal("50000.00"));
        validUser.setKycStatus(KycStatus.PENDING);

        when(userRepository.findAll()).thenReturn(Collections.singletonList(validUser));

        int repairedCount = legacyDataRepair.repairLegacyData();

        assertEquals(0, repairedCount);
        assertEquals(new BigDecimal("50000.00"), validUser.getDailyLimit());
        verify(userRepository, never()).save(validUser);
        verify(auditLogService, never()).log(anyString(), eq("LEGACY_DATA_REPAIRED"), anyString());
    }

    @Test
    @DisplayName("Running repair multiple times must be idempotent (zero duplicate saves or logs on second pass)")
    void testIdempotentExecution() {
        User legacyUser = new User("Rahul Sharma", "rahul@bankease.com", "pass", Role.CUSTOMER);
        legacyUser.setId(2L);
        legacyUser.setDailyLimit(BigDecimal.ZERO);

        List<User> list = new ArrayList<>();
        list.add(legacyUser);
        when(userRepository.findAll()).thenReturn(list);

        // First pass: repairs user
        int firstPassCount = legacyDataRepair.repairLegacyData();
        assertEquals(1, firstPassCount);
        assertEquals(defaultDailyLimit, legacyUser.getDailyLimit());
        verify(userRepository, times(1)).save(legacyUser);
        verify(auditLogService, times(1)).log(eq("rahul@bankease.com"), eq("LEGACY_DATA_REPAIRED"), anyString());

        // Second pass: user is already healthy -> must do nothing
        int secondPassCount = legacyDataRepair.repairLegacyData();
        assertEquals(0, secondPassCount);
        // Verify save and auditLog were not called again
        verify(userRepository, times(1)).save(legacyUser);
        verify(auditLogService, times(1)).log(eq("rahul@bankease.com"), eq("LEGACY_DATA_REPAIRED"), anyString());
    }

    @Test
    @DisplayName("Should repair null KYC status to PENDING for regular customers")
    void testRepairCustomerWithNullKyc() {
        User legacyUser = new User("Rahul Sharma", "rahul@bankease.com", "pass", Role.CUSTOMER);
        legacyUser.setId(2L);
        legacyUser.setKycStatus(null);

        when(userRepository.findAll()).thenReturn(Collections.singletonList(legacyUser));

        int repairedCount = legacyDataRepair.repairLegacyData();

        assertEquals(1, repairedCount);
        assertEquals(KycStatus.PENDING, legacyUser.getKycStatus());
        verify(userRepository).save(legacyUser);
        verify(auditLogService).log(eq("rahul@bankease.com"), eq("LEGACY_DATA_REPAIRED"), contains("kycStatus=PENDING"));
    }

    @Test
    @DisplayName("Should NOT modify Admin KYC status when it is already set")
    void testDoNotTouchAdminKyc() {
        User admin = new User("System Admin", "admin@bankease.com", "pass", Role.ADMIN);
        admin.setId(1L);
        admin.setKycStatus(KycStatus.VERIFIED);
        admin.setDailyLimit(new BigDecimal("500000.00"));

        when(userRepository.findAll()).thenReturn(Collections.singletonList(admin));

        int repairedCount = legacyDataRepair.repairLegacyData();

        assertEquals(0, repairedCount);
        assertEquals(KycStatus.VERIFIED, admin.getKycStatus());
        verify(userRepository, never()).save(admin);
    }

    @Test
    @DisplayName("Should repair negative failed login attempts to 0")
    void testRepairNegativeFailedAttempts() {
        User legacyUser = new User("Rahul Sharma", "rahul@bankease.com", "pass", Role.CUSTOMER);
        legacyUser.setId(2L);
        legacyUser.setFailedLoginAttempts(-3);

        when(userRepository.findAll()).thenReturn(Collections.singletonList(legacyUser));

        int repairedCount = legacyDataRepair.repairLegacyData();

        assertEquals(1, repairedCount);
        assertEquals(0, legacyUser.getFailedLoginAttempts());
        verify(userRepository).save(legacyUser);
        verify(auditLogService).log(eq("rahul@bankease.com"), eq("LEGACY_DATA_REPAIRED"), contains("failedLoginAttempts=0"));
    }
}

