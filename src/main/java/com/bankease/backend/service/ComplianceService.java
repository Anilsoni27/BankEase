package com.bankease.backend.service;

import com.bankease.backend.entity.KycStatus;
import com.bankease.backend.entity.OtpVerification;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.exception.BankeaseException;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.OtpVerificationRepository;
import com.bankease.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ComplianceService manages 2FA OTP simulation, KYC verification workflows,
 * document masking, daily limits, and account lockout management.
 */
@Service
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    private final OtpVerificationRepository otpVerificationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final boolean exposeOtpInResponse;
    private final SecureRandom secureRandom = new SecureRandom();

    public ComplianceService(OtpVerificationRepository otpVerificationRepository,
                             UserRepository userRepository,
                             AuditLogService auditLogService,
                             @Value("${bankease.otp.expose-in-response:false}") boolean exposeOtpInResponse) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.exposeOtpInResponse = exposeOtpInResponse;
    }

    /**
     * Generates a 6-digit OTP for 2-factor authentication / high value transfer authorization.
     * When exposeOtpInResponse is true (OTP_DEMO_MODE=true), the OTP code is returned in the API response.
     * When false (production default), it simulates SMS gateway delivery by logging to console only.
     */
    @Transactional
    public Map<String, Object> generateOtp(String userEmail, String purpose) {
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        OtpVerification otp = new OtpVerification(userEmail, otpCode, purpose, expiresAt);
        otpVerificationRepository.save(otp);

        auditLogService.log(userEmail, "OTP_GENERATED", "OTP generated for purpose: " + purpose);

        // Always log simulated SMS to server console for audit/demo
        log.info("[SIMULATED SMS] OTP for {}: {}", userEmail, otpCode);

        Map<String, Object> result = new HashMap<>();
        result.put("purpose", purpose);
        result.put("expiresAt", expiresAt);

        if (exposeOtpInResponse) {
            result.put("message", "OTP sent successfully. Valid for 5 minutes.");
            result.put("notice", "SIMULATED OTP: In production banking, this OTP is dispatched via SMS/Email gateway. Displayed in response when demo mode is enabled.");
            result.put("otpCode", otpCode); // Only exposed when OTP_DEMO_MODE=true
        } else {
            result.put("message", "OTP has been sent to your registered contact details. Valid for 5 minutes.");
        }

        return result;
    }

    /**
     * Verifies the submitted OTP code against active tokens in the database.
     */
    @Transactional
    public boolean verifyOtp(String userEmail, String otpCode, String purpose) {
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new BankeaseException("OTP code cannot be blank.");
        }

        OtpVerification otp = otpVerificationRepository
                .findTopByUserEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(userEmail, purpose)
                .orElseThrow(() -> new BankeaseException("No active OTP found. Please request a new one."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BankeaseException("OTP has expired. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(otpCode.trim())) {
            auditLogService.log(userEmail, "OTP_FAILED", "Incorrect OTP submitted for: " + purpose);
            throw new BankeaseException("Invalid OTP code. Please try again.");
        }

        otp.setUsed(true);
        otpVerificationRepository.save(otp);
        auditLogService.log(userEmail, "OTP_VERIFIED", "OTP verified successfully for: " + purpose);
        return true;
    }

    /**
     * Helper to mask sensitive government documents (e.g. Aadhaar / PAN)
     * e.g. 1234-5678-9012 becomes XXXX-XXXX-9012.
     */
    public String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.trim().isEmpty()) {
            return "XXXX-XXXX-XXXX";
        }
        String clean = documentNumber.replaceAll("[\\s-]", "");
        if (clean.length() <= 4) {
            return "XXXX" + clean;
        }
        String last4 = clean.substring(clean.length() - 4);
        return "XXXX-XXXX-" + last4;
    }

    /**
     * Submits customer KYC document for compliance review (sets status to PENDING).
     * Document number is masked before audit logging.
     */
    @Transactional
    public User submitKyc(User user, String documentType, String documentNumber) {
        String masked = maskDocumentNumber(documentNumber);
        user.setKycStatus(KycStatus.PENDING);
        User saved = userRepository.save(user);
        auditLogService.log(user.getEmail(), "KYC_SUBMITTED", "KYC documents submitted (" + documentType + ": " + masked + "). Awaiting admin verification.");
        return saved;
    }

    /**
     * Backward-compatible alias for updateKyc.
     */
    @Transactional
    public User updateKyc(User user, String documentType, String documentNumber) {
        return submitKyc(user, documentType, documentNumber);
    }

    /**
     * Admin workflow to formally approve (VERIFIED) or reject (REJECTED) customer KYC.
     */
    @Transactional
    public User verifyKycStatus(Long userId, KycStatus status, User adminUser) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Administrators can verify KYC status.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User " + userId + " not found"));

        user.setKycStatus(status);
        User saved = userRepository.save(user);
        auditLogService.log(adminUser.getEmail(), "KYC_" + status.name(), "Admin updated KYC status to " + status + " for user: " + user.getEmail());
        return saved;
    }

    /**
     * Updates daily transfer limit.
     */
    @Transactional
    public User updateDailyLimit(User user, BigDecimal newLimit) {
        if (newLimit == null || newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankeaseException("Daily limit must be greater than zero.");
        }
        user.setDailyLimit(newLimit);
        User saved = userRepository.save(user);
        auditLogService.log(user.getEmail(), "LIMIT_UPDATED", "Daily transfer limit updated to ₹" + newLimit);
        return saved;
    }

    /**
     * Admin unlocks a locked account.
     */
    @Transactional
    public User unlockUser(Long userId, User adminUser) {
        if (adminUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Administrators can unlock accounts.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        User saved = userRepository.save(user);
        auditLogService.log(adminUser.getEmail(), "ACCOUNT_UNLOCKED", "Unlocked customer account: " + user.getEmail());
        return saved;
    }
}
