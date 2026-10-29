package com.bankease.backend.repository;

import com.bankease.backend.entity.Account;
import com.bankease.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AccountRepository handles database persistence and lookups for Account entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Looks up an account using its unique 10-digit account number.
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Finds all accounts owned by a specific User entity.
     */
    List<Account> findByOwner(User owner);

    /**
     * Finds all accounts owned by a user ID.
     */
    List<Account> findByOwnerId(Long ownerId);

    /**
     * Checks if an account number already exists (used during generation to avoid collisions).
     */
    boolean existsByAccountNumber(String accountNumber);
}

