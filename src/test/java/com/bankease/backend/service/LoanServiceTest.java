package com.bankease.backend.service;

import com.bankease.backend.entity.*;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRecordService transactionRecordService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LoanService loanService;

    private User customer;
    private User admin;
    private Account account;

    @BeforeEach
    void setUp() {
        customer = new User("Rahul Sharma", "rahul@test.com", "pass", Role.CUSTOMER);
        admin = new User("System Admin", "admin@test.com", "pass", Role.ADMIN);
        account = new Account("1001001001", customer, new BigDecimal("10000.00"), AccountType.SAVINGS);
    }

    @Test
    @DisplayName("Should accurately calculate EMI using BigDecimal (₹100,000 at 12% for 12 months)")
    void testCalculateEmiWithBigDecimal() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("12.00");
        int tenureMonths = 12;

        BigDecimal emi = loanService.calculateEmi(principal, annualRate, tenureMonths);

        // Standard financial formula yields 8884.88
        assertEquals(new BigDecimal("8884.88"), emi);
    }

    @Test
    @DisplayName("Should handle 0% interest loan EMI without division by zero")
    void testCalculateEmiZeroInterest() {
        BigDecimal principal = new BigDecimal("60000.00");
        BigDecimal annualRate = BigDecimal.ZERO;
        int tenureMonths = 6;

        BigDecimal emi = loanService.calculateEmi(principal, annualRate, tenureMonths);

        assertEquals(new BigDecimal("10000.00"), emi);
    }

    @Test
    @DisplayName("Should return 0 for invalid loan inputs")
    void testCalculateEmiInvalidInputs() {
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), loanService.calculateEmi(null, new BigDecimal("10"), 12));
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), loanService.calculateEmi(new BigDecimal("1000"), null, 12));
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), loanService.calculateEmi(new BigDecimal("1000"), new BigDecimal("10"), 0));
    }

    @Test
    @DisplayName("Should apply for loan with calculated EMI and PENDING status")
    void testApplyLoan() {
        when(accountRepository.findByAccountNumber("1001001001")).thenReturn(Optional.of(account));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan loan = loanService.applyLoan(
                customer,
                "1001001001",
                LoanType.PERSONAL,
                new BigDecimal("50000.00"),
                new BigDecimal("11.50"),
                24,
                "Higher Education"
        );

        assertNotNull(loan);
        assertEquals(LoanStatus.PENDING, loan.getStatus());
        assertEquals(new BigDecimal("50000.00"), loan.getPrincipalAmount());
        verify(loanRepository).save(any(Loan.class));
        verify(auditLogService).log(eq("rahul@test.com"), eq("LOAN_APPLIED"), anyString());
    }

    @Test
    @DisplayName("Should approve loan and automatically disburse funds to customer account")
    void testApproveLoanDisbursement() {
        Loan loan = new Loan(customer, account, LoanType.PERSONAL, new BigDecimal("25000.00"), new BigDecimal("10.0"), 12, new BigDecimal("2197.94"), "Medical");
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal initialBalance = account.getBalance();
        Loan approvedLoan = loanService.approveLoan(1L, admin, "Approved by manager");

        assertEquals(LoanStatus.APPROVED, approvedLoan.getStatus());
        // Balance must increase by loan principal (10000 + 25000 = 35000)
        assertEquals(initialBalance.add(new BigDecimal("25000.00")), account.getBalance());
        verify(transactionRecordService).record(isNull(), eq(account), eq(new BigDecimal("25000.00")), eq(TransactionType.LOAN_DISBURSEMENT), eq(TransactionStatus.SUCCESS), anyString());
    }

    @Test
    @DisplayName("Should reject approval when non-admin user attempts it")
    void testNonAdminCannotApproveLoan() {
        assertThrows(UnauthorizedException.class, () -> loanService.approveLoan(1L, customer, "Sneaky approve"));
    }
}
