package com.bankease.backend.repository;

import com.bankease.backend.entity.Account;
import com.bankease.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TransactionRepository handles querying and recording transfer and deposit activity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Retrieves all transactions involving the given account as either sender OR receiver,
     * sorted with the newest transactions first.
     */
    List<Transaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(Account fromAccount, Account toAccount);

    /**
     * Retrieves all system transactions sorted newest first (for Admin overview).
     */
    List<Transaction> findAllByOrderByCreatedAtDesc();
}

