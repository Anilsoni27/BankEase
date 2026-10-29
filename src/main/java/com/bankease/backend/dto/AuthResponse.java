package com.bankease.backend.dto;

import java.util.List;

/**
 * AuthResponse is returned after successful registration or login.
 * It contains the generated in-memory session token along with user and account info.
 */
public class AuthResponse {

    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private List<AccountResponse> accounts;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String fullName, String email,
                        String role, List<AccountResponse> accounts) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.accounts = accounts;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<AccountResponse> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountResponse> accounts) {
        this.accounts = accounts;
    }
}

