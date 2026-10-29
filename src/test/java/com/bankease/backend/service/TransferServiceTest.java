package com.bankease.backend.service;

import com.bankease.backend.dto.TransferRequest;
import com.bankease.backend.dto.TransferResponse;
import com.bankease.backend.entity.*;
import com.bankease.backend.exception.BankeaseException;
import com.bankease.backend.exception.InsufficientBalanceException;
import com.bankease.backend.exception.InvalidTransferException;
import com.bankease.backend.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRecordService transactionRecordService;

    @Mock
    private ComplianceService complianceService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TransferService transferService;

    private User sender;
    private User receiver;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        sender = new User("Rahul Sharma", "rahul@bankease.com", "pass", Role.CUSTOMER);
        sender.setId(2L);
        sender.setDailyLimit(new BigDecimal("50000.00"));

        receiver = new User("Priya Patel", "priya@bankease.com", "pass", Role.CUSTOMER);
        receiver.setId(3L);

        fromAccount = new Account("1001001001", sender, new BigDecimal("10000.00"), AccountType.SAVINGS);
        fromAccount.setId(2L);

        toAccount = new Account("1002002002", receiver, new BigDecimal("10000.00"), AccountType.SAVINGS);
        toAccount.setId(3L);
    }

    @Test
    @DisplayName("Should successfully transfer funds between two accounts and update balances")
    void testSuccessfulTransfer() {
        TransferRequest req = new TransferRequest("1001001001", "1002002002", new BigDecimal("2500.00"), "Project share", null);

        when(accountRepository.findByAccountNumber("1001001001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("1002002002")).thenReturn(Optional.of(toAccount));

        Transaction mockTx = new Transaction(fromAccount, toAccount, new BigDecimal("2500.00"), TransactionType.TRANSFER, TransactionStatus.SUCCESS, "Project share");
        mockTx.setId(99L);
        when(transactionRecordService.record(eq(fromAccount), eq(toAccount), eq(new BigDecimal("2500.00")), eq(TransactionType.TRANSFER), eq(TransactionStatus.SUCCESS), anyString()))
                .thenReturn(mockTx);

        TransferResponse resp = transferService.transferFunds(sender, req);

        assertNotNull(resp);
        assertEquals(99L, resp.getTransactionId());
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals(new BigDecimal("7500.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("12500.00"), toAccount.getBalance());

        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
    }

    @Test
    @DisplayName("Should fail when balance is insufficient and record a FAILED transaction attempt")
    void testInsufficientBalanceFailsAndRecords() {
        TransferRequest req = new TransferRequest("1001001001", "1002002002", new BigDecimal("15000.00"), "Too large", null);

        when(accountRepository.findByAccountNumber("1001001001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("1002002002")).thenReturn(Optional.of(toAccount));

        assertThrows(InsufficientBalanceException.class, () -> transferService.transferFunds(sender, req));

        // Balance should not have changed
        assertEquals(new BigDecimal("10000.00"), fromAccount.getBalance());
        // FAILED transaction record must be created for compliance
        verify(transactionRecordService).record(eq(fromAccount), eq(toAccount), eq(new BigDecimal("15000.00")), eq(TransactionType.TRANSFER), eq(TransactionStatus.FAILED), anyString());
    }

    @Test
    @DisplayName("Should reject self-transfer to identical account number")
    void testSelfTransferRejected() {
        TransferRequest req = new TransferRequest("1001001001", "1001001001", new BigDecimal("500.00"), "Self", null);

        when(accountRepository.findByAccountNumber("1001001001")).thenReturn(Optional.of(fromAccount));

        assertThrows(InvalidTransferException.class, () -> transferService.transferFunds(sender, req));
        verify(transactionRecordService).record(eq(fromAccount), eq(fromAccount), eq(new BigDecimal("500.00")), eq(TransactionType.TRANSFER), eq(TransactionStatus.FAILED), anyString());
    }

    @Test
    @DisplayName("Should reject transfer exceeding daily compliance limit")
    void testDailyLimitExceededRejected() {
        // Daily limit is 50,000; attempting 60,000
        fromAccount.setBalance(new BigDecimal("100000.00"));
        TransferRequest req = new TransferRequest("1001001001", "1002002002", new BigDecimal("60000.00"), "Over limit", null);

        when(accountRepository.findByAccountNumber("1001001001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("1002002002")).thenReturn(Optional.of(toAccount));

        BankeaseException ex = assertThrows(BankeaseException.class, () -> transferService.transferFunds(sender, req));
        assertTrue(ex.getMessage().contains("daily transfer limit"));
        verify(auditLogService).log(eq("rahul@bankease.com"), eq("TRANSFER_LIMIT_EXCEEDED"), anyString());
    }

    @Test
    @DisplayName("Should require OTP for high-value transfer (>= ₹25,000)")
    void testHighValueTransferRequiresOtp() {
        TransferRequest req = new TransferRequest("1001001001", "1002002002", new BigDecimal("30000.00"), "High value", null);

        when(accountRepository.findByAccountNumber("1001001001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("1002002002")).thenReturn(Optional.of(toAccount));

        BankeaseException ex = assertThrows(BankeaseException.class, () -> transferService.transferFunds(sender, req));
        assertTrue(ex.getMessage().contains("requires 2-factor OTP"));
    }
}
