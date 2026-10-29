package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.entity.AuditLog;
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

@RestController
@RequestMapping("/api/compliance")
@CrossOrigin(origins = "*")
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

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String purpose = body.getOrDefault("purpose", "TWO_FACTOR_AUTH");
        Map<String, Object> res = complianceService.generateOtp(email, purpose);
        return ResponseEntity.ok(ApiResponse.success("OTP generated successfully", res));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otpCode = body.get("otpCode");
        String purpose = body.getOrDefault("purpose", "TWO_FACTOR_AUTH");
        boolean valid = complianceService.verifyOtp(email, otpCode, purpose);
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
        String docType = body.getOrDefault("documentType", "Aadhaar / PAN");
        String docNumber = body.getOrDefault("documentNumber", "XXXX-XXXX-XXXX");
        User updated = complianceService.updateKyc(user, docType, docNumber);
        return ResponseEntity.ok(ApiResponse.success("KYC verified successfully.", updated));
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
        User unlocked = complianceService.unlockUser(userId, adminUser);
        return ResponseEntity.ok(ApiResponse.success("User account unlocked.", unlocked));
    }
}
