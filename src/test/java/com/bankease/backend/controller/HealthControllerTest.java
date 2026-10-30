package com.bankease.backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    @DisplayName("Health check should return HTTP 200 with status UP")
    void testCheckHealthReturnsUp() {
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("service"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
}

