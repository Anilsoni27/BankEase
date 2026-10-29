package com.bankease.backend.repository;

import com.bankease.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository provides database CRUD operations and query methods for User entities.
 *
 * Annotations explained:
 * - @Repository: Indicates that this interface acts as a Data Access Object (DAO).
 *   Spring Data JPA automatically generates the SQL implementation at runtime.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Spring Data JPA derives the SQL query automatically from the method name:
     * "SELECT * FROM users WHERE email = ?"
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user already exists with the given email.
     */
    boolean existsByEmail(String email);
}

