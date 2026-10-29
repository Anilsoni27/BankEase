package com.bankease.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account Entity represents a bank account owned by a User.
 *
 * Annotations explained:
 * - @Entity: Marks this class as a JPA entity managed by Hibernate.
 * - @Table(name = "accounts"): Maps this entity to the "accounts" database table.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique 10-digit account number auto-generated upon account opening.
     */
    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    /**
     * @ManyToOne: Establishes a Many-to-One relationship between accounts and users
     * (one user can own one or more accounts).
     * @JoinColumn(name = "user_id", nullable = false): Specifies the foreign key column name
     * referencing the 'users' table primary key.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /**
     * BigDecimal is used for financial amounts to avoid floating-point rounding errors.
     * precision = 15, scale = 2 represents numbers up to 13 digits with 2 decimal places (e.g. 10000.00).
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Account() {
    }

    public Account(String accountNumber, User owner, BigDecimal balance, AccountType accountType) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = balance;
        this.accountType = accountType;
    }

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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

