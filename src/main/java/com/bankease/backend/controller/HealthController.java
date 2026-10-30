package com.bankease.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HealthController provides a lightweight, unauthenticated health-check probe
 * for container orchestrators (Docker, Kubernetes), cloud platform health monitors
 * (Render, Railway, AWS ALB, Google Cloud Run), and uptime monitors.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "BankEase NextGen Digital Banking Suite");
        status.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(status);
    }
}

