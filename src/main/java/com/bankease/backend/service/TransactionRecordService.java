package com.bankease.backend.service;

import com.bankease.backend.entity.Account;
import com.bankease.backend.entity.Transaction;
import com.bankease.backend.entity.TransactionStatus;
import com.bankease.backend.entity.TransactionType;
import com.bankease.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * TransactionRecordService writes audit logs for transfers and deposits.
 *
 * Annotations explained:
 * - @Service: Marks this class as a Spring Service holding business operations.
 * - @Transactional(propagation = Propagation.REQUIRES_NEW):
 *   Starts a BRAND NEW, independent database transaction whenever this method is called.
 *   Even if the outer calling transaction rolls back (e.g. due to InsufficientBalanceException),
 *   the FAILED audit record created here is safely committed to the MySQL database!
 */
@Service
public class TransactionRecordService {

    private final TransactionRepository transactionRepository;

    public TransactionRecordService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Records a financial transaction attempt in its own independent database transaction.
     * Guarantees that every attempt (SUCCESS or FAILED) is persisted for banking compliance.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction record(Account fromAccount, Account toAccount,
                              BigDecimal amount, TransactionType type,
                              TransactionStatus status, String description) {
        Transaction tx = new Transaction(fromAccount, toAccount, amount, type, status, description);
        return transactionRepository.save(tx);
    }
}

