package com.bankease.backend.controller;

import com.bankease.backend.dto.AccountResponse;
import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccountController exposes endpoints to query bank account details and balances.
 */
@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;
    private final AuthHelper authHelper;

    public AccountController(AccountService accountService, AuthHelper authHelper) {
        this.accountService = accountService;
        this.authHelper = authHelper;
    }

    /**
     * Feature 3: Get my accounts and balance
     * Reads session token from Authorization header and returns all accounts owned by the user.
     */
    @GetMapping("/my-accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = authHelper.authenticate(authHeader);
        List<AccountResponse> accounts = accountService.getAccountsByUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Accounts fetched successfully.", accounts));
    }

    /**
     * Get account details by account number.
     * Enforces that the caller owns the account, unless the caller is an ADMIN.
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountDetails(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = authHelper.authenticate(authHeader);
        AccountResponse account = accountService.getAccountResponseByNumber(accountNumber);

        boolean isOwner = account.getOwnerEmail().equalsIgnoreCase(currentUser.getEmail());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Access denied: You do not own this account.");
        }

        return ResponseEntity.ok(ApiResponse.success("Account details fetched successfully.", account));
    }
}

