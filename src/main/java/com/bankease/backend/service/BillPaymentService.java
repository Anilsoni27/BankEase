package com.bankease.backend.service;

import com.bankease.backend.entity.*;
import com.bankease.backend.exception.InsufficientBalanceException;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.repository.AccountRepository;
import com.bankease.backend.repository.BillPayeeRepository;
import com.bankease.backend.repository.BillPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillPaymentService {

    private final BillPayeeRepository payeeRepository;
    private final BillPaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRecordService transactionRecordService;
    private final AuditLogService auditLogService;

    public BillPaymentService(BillPayeeRepository payeeRepository,
                              BillPaymentRepository paymentRepository,
                              AccountRepository accountRepository,
                              TransactionRecordService transactionRecordService,
                              AuditLogService auditLogService) {
        this.payeeRepository = payeeRepository;
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.transactionRecordService = transactionRecordService;
        this.auditLogService = auditLogService;
    }

    public List<BillPayee> getUserPayees(User user) {
        return payeeRepository.findByUserId(user.getId());
    }

    @Transactional
    public BillPayee addPayee(User user, String payeeName, BillCategory category, String consumerNumber, String nickname) {
        BillPayee payee = new BillPayee(user, payeeName.trim(), category, consumerNumber.trim(), nickname);
        BillPayee saved = payeeRepository.save(payee);
        auditLogService.log(user.getEmail(), "PAYEE_ADDED", "Added bill payee: " + payeeName + " (" + category + ")");
        return saved;
    }

    @Transactional
    public BillPayment payBill(User user, String accountNumber, String payeeName, BillCategory category,
                               String consumerNumber, BigDecimal amount) {
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
                    TransactionType.BILL_PAY,
                    TransactionStatus.FAILED,
                    "Bill payment failed: Insufficient balance for " + payeeName
            );
            throw new InsufficientBalanceException("Insufficient balance for bill payment.");
        }

        // Debit account
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // Record Transaction
        transactionRecordService.record(
                account,
                null,
                amount,
                TransactionType.BILL_PAY,
                TransactionStatus.SUCCESS,
                "Utility Bill Pay: " + payeeName + " (" + category + ") - Consumer #" + consumerNumber
        );

        // Save BillPayment
        BillPayment payment = new BillPayment(
                user,
                account,
                payeeName,
                category,
                consumerNumber,
                amount,
                BillStatus.PAID,
                false,
                null
        );
        payment.setPaidAt(LocalDateTime.now());
        BillPayment saved = paymentRepository.save(payment);

        auditLogService.log(user.getEmail(), "BILL_PAID", "Paid ₹" + amount + " to " + payeeName);
        return saved;
    }

    @Transactional
    public BillPayment scheduleBill(User user, String accountNumber, String payeeName, BillCategory category,
                                    String consumerNumber, BigDecimal amount, LocalDate scheduledDate) {
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " not found."));

        BillPayment scheduled = new BillPayment(
                user,
                account,
                payeeName,
                category,
                consumerNumber,
                amount,
                BillStatus.SCHEDULED,
                true,
                scheduledDate
        );
        BillPayment saved = paymentRepository.save(scheduled);
        auditLogService.log(user.getEmail(), "BILL_SCHEDULED", "Scheduled bill payment of ₹" + amount + " to " + payeeName + " for " + scheduledDate);
        return saved;
    }

    public List<BillPayment> getPaymentHistory(User user) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}

