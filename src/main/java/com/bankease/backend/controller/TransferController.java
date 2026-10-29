package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.dto.TransferRequest;
import com.bankease.backend.dto.TransferResponse;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * TransferController provides REST endpoints to initiate atomic fund transfers.
 */
@RestController
@RequestMapping("/api/transfers")
@CrossOrigin(origins = "*")
public class TransferController {

    private final TransferService transferService;
    private final AuthHelper authHelper;

    public TransferController(TransferService transferService, AuthHelper authHelper) {
        this.transferService = transferService;
        this.authHelper = authHelper;
    }

    /**
     * Feature 4: Fund transfer between two accounts by account number.
     *
     * Validations performed in service:
     * - amount > 0
     * - Sender must own the source account
     * - Cannot transfer to same account
     * - Receiver exists
     * - Sender has sufficient balance
     *
     * Every attempt (SUCCESS or FAILED) is recorded in the transactions table.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody TransferRequest request) {

        User currentUser = authHelper.authenticate(authHeader);
        TransferResponse response = transferService.transferFunds(currentUser, request);

        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully.", response));
    }
}

