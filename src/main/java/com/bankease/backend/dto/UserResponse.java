package com.bankease.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UserResponse represents user details with their associated bank accounts,
 * KYC status, and security lock state.
 */
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String kycStatus;
    private boolean accountLocked;
    private BigDecimal dailyLimit;
    private LocalDateTime createdAt;
    private List<AccountResponse> accounts;

    public UserResponse() {
    }

    public UserResponse(Long id, String fullName, String email, String role,
                        String kycStatus, boolean accountLocked, BigDecimal dailyLimit,
                        LocalDateTime createdAt, List<AccountResponse> accounts) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.kycStatus = kycStatus;
        this.accountLocked = accountLocked;
        this.dailyLimit = dailyLimit;
        this.createdAt = createdAt;
        this.accounts = accounts;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public boolean isAccountLocked() { return accountLocked; }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }

    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<AccountResponse> getAccounts() { return accounts; }
    public void setAccounts(List<AccountResponse> accounts) { this.accounts = accounts; }
}
