package com.bankease.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User Entity represents registered individuals in the BankEase system.
 *
 * Annotations explained:
 * - @Entity: Tells JPA / Hibernate that this class maps to a database table.
 * - @Table(name = "users"): Names the table "users" in MySQL (avoiding the SQL reserved word "user").
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * @Id: Declares this field as the primary key of the table.
     * @GeneratedValue(strategy = GenerationType.IDENTITY): Delegates primary key generation
     * to the MySQL database AUTO_INCREMENT column.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Column(nullable = false): Ensures this column cannot be NULL in the database schema.
     */
    @Column(nullable = false, length = 100)
    private String fullName;

    /**
     * @Column(nullable = false, unique = true): Enforces that every user has a distinct email address.
     */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /**
     * Stores the BCrypt hashed password (never plain text!).
     */
    @Column(nullable = false)
    private String password;

    /**
     * @Enumerated(EnumType.STRING): Persists the Enum as a readable string ("CUSTOMER" or "ADMIN")
     * rather than an index number (0 or 1), making database inspection and migrations safer.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default no-argument constructor is required by JPA/Hibernate for entity instantiation.
     */
    public User() {
    }

    public User(String fullName, String email, String password, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    /**
     * @PrePersist: Automatically invoked by JPA right before inserting the record into MySQL.
     * Sets the creation timestamp if not already provided.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

