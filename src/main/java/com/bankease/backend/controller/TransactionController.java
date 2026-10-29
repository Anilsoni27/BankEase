package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.dto.TransactionResponse;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TransactionController provides endpoints to query account statement and transfer history.
 */
@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthHelper authHelper;

    public TransactionController(TransactionService transactionService, AuthHelper authHelper) {
        this.transactionService = transactionService;
        this.authHelper = authHelper;
    }

    /**
     * Feature 5: Transaction history for an account (latest first).
     * Returns all debit, credit, and failed transaction attempts for the account.
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountTransactions(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("Authorization") String authHeader) {

        User currentUser = authHelper.authenticate(authHeader);
        List<TransactionResponse> history = transactionService.getAccountTransactionHistory(accountNumber, currentUser);

        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved successfully.", history));
    }
}

