package com.bankease.backend.controller;

import com.bankease.backend.dto.AccountResponse;
import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.dto.TransactionResponse;
import com.bankease.backend.dto.UserResponse;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminController provides management endpoints accessible strictly to users with role ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthHelper authHelper;

    public AdminController(AdminService adminService, AuthHelper authHelper) {
        this.adminService = adminService;
        this.authHelper = authHelper;
    }

    /**
     * Feature 6: List all users and their registered accounts (ADMIN role only).
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestHeader("Authorization") String authHeader) {
        User adminUser = authHelper.authenticate(authHeader);
        List<UserResponse> users = adminService.getAllUsers(adminUser);
        return ResponseEntity.ok(ApiResponse.success("All users retrieved successfully.", users));
    }

    /**
     * Feature 6: List all bank accounts across the entire system (ADMIN role only).
     */
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts(
            @RequestHeader("Authorization") String authHeader) {
        User adminUser = authHelper.authenticate(authHeader);
        List<AccountResponse> accounts = adminService.getAllAccounts(adminUser);
        return ResponseEntity.ok(ApiResponse.success("All accounts retrieved successfully.", accounts));
    }

    /**
     * Additional Admin Feature: View all transactions across the entire system.
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @RequestHeader("Authorization") String authHeader) {
        User adminUser = authHelper.authenticate(authHeader);
        List<TransactionResponse> transactions = adminService.getAllTransactions(adminUser);
        return ResponseEntity.ok(ApiResponse.success("All system transactions retrieved successfully.", transactions));
    }
}

