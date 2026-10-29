package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.entity.AuditLog;
import com.bankease.backend.entity.KycStatus;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.AuditLogService;
import com.bankease.backend.service.ComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ComplianceController provides endpoints for:
 * - 2FA OTP simulation (bound to authenticated user session)
 * - KYC submission and Admin verification
 * - Daily transfer limit adjustments
 * - Audit logs and Account unlock
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final AuditLogService auditLogService;
    private final AuthHelper authHelper;

    public ComplianceController(ComplianceService complianceService,
                                AuditLogService auditLogService,
                                AuthHelper authHelper) {
        this.complianceService = complianceService;
        this.auditLogService = auditLogService;
        this.authHelper = authHelper;
    }

    /**
     * Generates a simulated OTP for the currently authenticated user.
     * Binding to the user session prevents third-party OTP generation/spoofing.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        User user = authHelper.authenticate(authHeader);
        String purpose = (body != null && body.containsKey("purpose")) ? body.get("purpose") : "TWO_FACTOR_AUTH";
        Map<String, Object> res = complianceService.generateOtp(user.getEmail(), purpose);
        return ResponseEntity.ok(ApiResponse.success("OTP generated successfully", res));
    }

    /**
     * Verifies the submitted OTP for the authenticated user session.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        User user = authHelper.authenticate(authHeader);
        String otpCode = body != null ? body.get("otpCode") : null;
        String purpose = (body != null && body.containsKey("purpose")) ? body.get("purpose") : "TWO_FACTOR_AUTH";
        boolean valid = complianceService.verifyOtp(user.getEmail(), otpCode, purpose);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully.", valid));
    }

    @GetMapping("/kyc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKyc(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        return ResponseEntity.ok(ApiResponse.success("KYC details fetched", Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "kycStatus", user.getKycStatus(),
                "dailyLimit", user.getDailyLimit(),
                "isAccountLocked", user.isAccountLocked()
        )));
    }

    @PostMapping("/update-kyc")
    public ResponseEntity<ApiResponse<User>> updateKyc(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        User user = authHelper.authenticate(authHeader);
        String docType = body != null ? body.getOrDefault("documentType", "Aadhaar / PAN") : "Aadhaar / PAN";
        String docNumber = body != null ? body.getOrDefault("documentNumber", "XXXX-XXXX-XXXX") : "XXXX-XXXX-XXXX";
        User updated = complianceService.submitKyc(user, docType, docNumber);
        return ResponseEntity.ok(ApiResponse.success("KYC submitted successfully. Status is PENDING admin verification.", updated));
    }

    /**
     * Admin endpoint: Approve or Reject a user's KYC status.
     */
    @PostMapping("/admin/verify-kyc/{userId}")
    public ResponseEntity<ApiResponse<User>> adminVerifyKyc(
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        User adminUser = authHelper.authenticate(authHeader);
        authHelper.requireAdmin(adminUser);
        String statusStr = (body != null && body.containsKey("status")) ? body.get("status") : "VERIFIED";
        KycStatus status = KycStatus.valueOf(statusStr.toUpperCase());
        User updated = complianceService.verifyKycStatus(userId, status, adminUser);
        return ResponseEntity.ok(ApiResponse.success("User KYC status updated to " + status + ".", updated));
    }

    @PostMapping("/update-limit")
    public ResponseEntity<ApiResponse<User>> updateLimit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        User user = authHelper.authenticate(authHeader);
        BigDecimal newLimit = new BigDecimal(body.get("dailyLimit").toString());
        User updated = complianceService.updateDailyLimit(user, newLimit);
        return ResponseEntity.ok(ApiResponse.success("Daily limit updated.", updated));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(ApiResponse.success("System audit logs", auditLogService.getAllLogs()));
        } else {
            return ResponseEntity.ok(ApiResponse.success("User audit logs", auditLogService.getUserLogs(user.getEmail())));
        }
    }

    @PostMapping("/unlock-user/{userId}")
    public ResponseEntity<ApiResponse<User>> unlockUser(
            @PathVariable("userId") Long userId,
            @RequestHeader("Authorization") String authHeader) {
        User adminUser = authHelper.authenticate(authHeader);
        authHelper.requireAdmin(adminUser);
        User unlocked = complianceService.unlockUser(userId, adminUser);
        return ResponseEntity.ok(ApiResponse.success("User account unlocked.", unlocked));
    }
}
