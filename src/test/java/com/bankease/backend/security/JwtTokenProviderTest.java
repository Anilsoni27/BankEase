package com.bankease.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "BankEaseTestSecretKeyForUnitTestsMustBeAtLeast32BytesLong2026!@#$%^&*()_+";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT token and parse claims")
    void testGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken(101L, "user@bankease.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("user@bankease.com", jwtTokenProvider.getEmailFromToken(token));
        assertEquals(101L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("CUSTOMER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens")
    void testInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.string"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should reject token signed with different key")
    void testTamperedSignatureToken() {
        JwtTokenProvider anotherProvider = new JwtTokenProvider(
                "DifferentSecretKeyMustAlsoBeAtLeast32BytesLongForHmacSha256Signature2026!@#$",
                expirationMs
        );
        String foreignToken = anotherProvider.generateToken(102L, "hacker@test.com", "ADMIN");

        assertFalse(jwtTokenProvider.validateToken(foreignToken));
    }

    @Test
    @DisplayName("Should fail-fast if JWT secret is null, blank, or shorter than 32 characters")
    void testFailFastOnWeakSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null, expirationMs));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", expirationMs));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("   ", expirationMs));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("short_secret_under_32_bytes", expirationMs));
    }
}
