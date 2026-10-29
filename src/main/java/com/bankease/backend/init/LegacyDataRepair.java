package com.bankease.backend.init;

import com.bankease.backend.entity.KycStatus;
import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.repository.UserRepository;
import com.bankease.backend.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * LegacyDataRepair runs on application startup to repair historical user rows
 * whose compliance attributes (e.g. dailyLimit, kycStatus, failedLoginAttempts)
 * are null, zero, or uninitialized from older database schema versions.
 *
 * It is completely independent of bankease.seed.enabled and is strictly idempotent.
 */
@Component
@Order(1)
public class LegacyDataRepair implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataRepair.class);

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final BigDecimal defaultDailyLimit;

    public LegacyDataRepair(UserRepository userRepository,
                            AuditLogService auditLogService,
                            @Value("${bankease.compliance.default-daily-limit:100000.00}") BigDecimal defaultDailyLimit) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.defaultDailyLimit = (defaultDailyLimit != null && defaultDailyLimit.compareTo(BigDecimal.ZERO) > 0)
                ? defaultDailyLimit
                : User.DEFAULT_DAILY_LIMIT;
    }

    @Override
    @Transactional
    public void run(String... args) {
        repairLegacyData();
    }

    /**
     * Inspects all users and applies repairs to missing or zero compliance fields.
     *
     * @return Number of user records that were repaired.
     */
    @Transactional
    public int repairLegacyData() {
        List<User> users = userRepository.findAll();
        int repairedCount = 0;

        for (User user : users) {
            List<String> repairedFields = new ArrayList<>();

            // 1. Repair dailyLimit if null or <= 0
            if (user.getDailyLimit() == null || user.getDailyLimit().compareTo(BigDecimal.ZERO) <= 0) {
                user.setDailyLimit(defaultDailyLimit);
                repairedFields.add("dailyLimit=" + defaultDailyLimit);
            }

            // 2. Repair kycStatus if null (Do not modify admin KYC)
            if (user.getKycStatus() == null) {
                if (user.getRole() != Role.ADMIN) {
                    user.setKycStatus(KycStatus.PENDING);
                    repairedFields.add("kycStatus=PENDING");
                }
            }

            // 3. Repair failedLoginAttempts if negative
            if (user.getFailedLoginAttempts() < 0) {
                user.setFailedLoginAttempts(0);
                repairedFields.add("failedLoginAttempts=0");
            }

            // 4. Save and audit log only if at least one field required repair
            if (!repairedFields.isEmpty()) {
                userRepository.save(user);
                repairedCount++;
                String details = "User " + user.getId() + " (" + user.getEmail() + "): repaired fields [" + String.join(", ", repairedFields) + "]";
                auditLogService.log(user.getEmail(), "LEGACY_DATA_REPAIRED", details);
                log.info("Repaired legacy compliance fields for user {}: {}", user.getEmail(), repairedFields);
            }
        }

        if (repairedCount > 0) {
            log.info("LegacyDataRepair: successfully updated {} legacy user account(s).", repairedCount);
        }

        return repairedCount;
    }
}

