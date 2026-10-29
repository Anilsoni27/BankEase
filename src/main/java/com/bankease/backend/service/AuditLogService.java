package com.bankease.backend.service;

import com.bankease.backend.entity.AuditLog;
import com.bankease.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String email, String action, String details, HttpServletRequest request) {
        String ip = "127.0.0.1";
        if (request != null) {
            String header = request.getHeader("X-Forwarded-For");
            ip = (header != null && !header.isEmpty()) ? header.split(",")[0] : request.getRemoteAddr();
        }
        AuditLog log = new AuditLog(email, action, ip, details);
        auditLogRepository.save(log);
    }

    public void log(String email, String action, String details) {
        log(email, action, details, null);
    }

    public List<AuditLog> getUserLogs(String email) {
        return auditLogRepository.findByUserEmailOrderByTimestampDesc(email);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}

