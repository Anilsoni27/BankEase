package com.bankease.backend.service;

import com.bankease.backend.entity.*;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransactionRecordService transactionRecordService;
    private final AuditLogService auditLogService;

    public LoanService(LoanRepository loanRepository,
                       AccountRepository accountRepository,
                       TransactionRecordService transactionRecordService,
                       AuditLogService auditLogService) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.transactionRecordService = transactionRecordService;
        this.auditLogService = auditLogService;
    }

    /**
     * Calculates monthly Equated Monthly Installment (EMI) using high-precision BigDecimal arithmetic.
     * Formula: E = [P x r x (1+r)^n] / [(1+r)^n - 1]
     * where:
     * P = principal loan amount
     * r = monthly interest rate (annual rate / 12 / 100)
     * n = tenure in months
     */
    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || annualInterestRate == null || tenureMonths <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        // High precision math context with 16 significant digits to eliminate floating point rounding errors
        java.math.MathContext mc = new java.math.MathContext(16, RoundingMode.HALF_UP);

        // Monthly interest rate: r = annualInterestRate / 1200
        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(1200), mc);

        // (1 + r)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, mc);

        // (1 + r)^n using BigDecimal pow
        BigDecimal onePlusRPowN = onePlusR.pow(tenureMonths, mc);

        // Numerator = P * r * (1 + r)^n
        BigDecimal numerator = principal.multiply(monthlyRate, mc).multiply(onePlusRPowN, mc);

        // Denominator = (1 + r)^n - 1
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, mc);

        // Round final EMI to 2 decimal places with HALF_UP rounding
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public Loan applyLoan(User user, String accountNumber, LoanType loanType,
                          BigDecimal principalAmount, BigDecimal annualInterestRate,
                          int tenureMonths, String purpose) {
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " not found."));

        BigDecimal emi = calculateEmi(principalAmount, annualInterestRate, tenureMonths);

        Loan loan = new Loan(
                user,
                account,
                loanType,
                principalAmount,
                annualInterestRate,
                tenureMonths,
                emi,
                purpose
        );
        Loan saved = loanRepository.save(loan);
        auditLogService.log(user.getEmail(), "LOAN_APPLIED", "Applied for " + loanType + " loan of ₹" + principalAmount);
        return saved;
    }

    public List<Loan> getUserLoans(User user) {
        return loanRepository.findByUserIdOrderByAppliedAtDesc(user.getId());
    }

    public List<Loan> getAllLoans(User adminUser) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin privileges required to inspect all loans.");
        }
        return loanRepository.findAllByOrderByAppliedAtDesc();
    }

    /**
     * Approves loan and automatically disburses the loan amount into the borrower's account!
     */
    @Transactional
    public Loan approveLoan(Long loanId, User adminUser, String remarks) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin privileges required to approve loans.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application " + loanId + " not found."));

        loan.setStatus(LoanStatus.APPROVED);
        loan.setAdminRemarks(remarks != null ? remarks : "Approved by Loan Officer.");
        loan.setReviewedAt(LocalDateTime.now());
        Loan saved = loanRepository.save(loan);

        // Disburse funds directly into customer's account
        Account account = loan.getAccount();
        account.setBalance(account.getBalance().add(loan.getPrincipalAmount()));
        accountRepository.save(account);

        // Record Transaction
        transactionRecordService.record(
                null,
                account,
                loan.getPrincipalAmount(),
                TransactionType.LOAN_DISBURSEMENT,
                TransactionStatus.SUCCESS,
                "Loan Disbursed: " + loan.getLoanType() + " Loan #" + loan.getId() + " - Principal: ₹" + loan.getPrincipalAmount()
        );

        auditLogService.log(adminUser.getEmail(), "LOAN_APPROVED", "Approved & disbursed loan #" + loanId + " of ₹" + loan.getPrincipalAmount() + " to " + loan.getUser().getEmail());
        return saved;
    }

    @Transactional
    public Loan rejectLoan(Long loanId, User adminUser, String remarks) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin privileges required to reject loans.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application " + loanId + " not found."));

        loan.setStatus(LoanStatus.REJECTED);
        loan.setAdminRemarks(remarks != null ? remarks : "Rejected by Loan Officer.");
        loan.setReviewedAt(LocalDateTime.now());
        Loan saved = loanRepository.save(loan);

        auditLogService.log(adminUser.getEmail(), "LOAN_REJECTED", "Rejected loan #" + loanId + " for " + loan.getUser().getEmail());
        return saved;
    }
}

