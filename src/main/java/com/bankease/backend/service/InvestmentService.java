package com.bankease.backend.service;

import com.bankease.backend.entity.*;
import com.bankease.backend.exception.InsufficientBalanceException;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.InvestmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRecordService transactionRecordService;
    private final AuditLogService auditLogService;

    public InvestmentService(InvestmentRepository investmentRepository,
                             AccountRepository accountRepository,
                             TransactionRecordService transactionRecordService,
                             AuditLogService auditLogService) {
        this.investmentRepository = investmentRepository;
        this.accountRepository = accountRepository;
        this.transactionRecordService = transactionRecordService;
        this.auditLogService = auditLogService;
    }

    /**
     * Calculates FD maturity value compounded quarterly.
     * Formula: A = P * (1 + r/400)^(4 * t)
     */
    public BigDecimal calculateFdMaturity(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        double p = principal.doubleValue();
        double r = annualRate.doubleValue();
        double t = tenureMonths / 12.0;

        double maturity = p * Math.pow(1 + (r / 400.0), 4 * t);
        return BigDecimal.valueOf(maturity).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public Investment createInvestment(User user, String accountNumber, InvestmentType type,
                                       String planName, BigDecimal amount, BigDecimal returnRate, int tenureMonths) {
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " not found."));

        if (!account.getOwner().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Account does not belong to you.");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            transactionRecordService.record(
                    account,
                    null,
                    amount,
                    TransactionType.INVESTMENT_DEBIT,
                    TransactionStatus.FAILED,
                    "Investment failed: Insufficient balance for " + planName
            );
            throw new InsufficientBalanceException("Insufficient balance to initiate investment.");
        }

        // Debit account
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // Calculate maturity
        BigDecimal maturity = calculateFdMaturity(amount, returnRate, tenureMonths);
        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(tenureMonths);

        // Record Transaction
        transactionRecordService.record(
                account,
                null,
                amount,
                TransactionType.INVESTMENT_DEBIT,
                TransactionStatus.SUCCESS,
                "Investment initiated: " + planName + " (" + type + ") - Maturity: ₹" + maturity
        );

        Investment investment = new Investment(
                user,
                account,
                type,
                planName,
                amount,
                returnRate,
                tenureMonths,
                maturity,
                startDate,
                maturityDate
        );
        Investment saved = investmentRepository.save(investment);
        auditLogService.log(user.getEmail(), "INVESTMENT_CREATED", "Invested ₹" + amount + " in " + planName);
        return saved;
    }

    public List<Investment> getUserInvestments(User user) {
        return investmentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Investment closeInvestment(Long investmentId, User user) {
        Investment inv = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment " + investmentId + " not found."));

        if (!inv.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Investment does not belong to you.");
        }

        if (inv.getStatus() == InvestmentStatus.CLOSED) {
            throw new ResourceNotFoundException("Investment is already closed.");
        }

        inv.setStatus(InvestmentStatus.CLOSED);
        Investment saved = investmentRepository.save(inv);

        // Credit proceeds back to account
        Account account = inv.getAccount();
        account.setBalance(account.getBalance().add(inv.getMaturityAmount()));
        accountRepository.save(account);

        // Record Transaction
        transactionRecordService.record(
                null,
                account,
                inv.getMaturityAmount(),
                TransactionType.INVESTMENT_CREDIT,
                TransactionStatus.SUCCESS,
                "Investment Closed / Matured: " + inv.getPlanName() + " - Payout: ₹" + inv.getMaturityAmount()
        );

        auditLogService.log(user.getEmail(), "INVESTMENT_CLOSED", "Closed investment #" + investmentId + ". Credited ₹" + inv.getMaturityAmount());
        return saved;
    }
}

