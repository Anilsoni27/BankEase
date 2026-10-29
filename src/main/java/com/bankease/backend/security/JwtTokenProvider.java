package com.bankease.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtTokenProvider generates, signs, and parses JSON Web Tokens (JWT)
 * using HMAC-SHA256 for secure, stateless authentication.
 */
@Component
public class JwtTokenProvider {

    // 256-bit secret key for HMAC-SHA256 signing
    private static final String JWT_SECRET = "BankEaseSecureNextGenNetBankingSecretKey2026!@#$%^&*()_+";
    private static final long JWT_EXPIRATION_MS = 86400000L; // 24 hours

    private final SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * Generates a signed JWT containing user claims.
     */
    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION_MS);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extracts email (subject) from JWT.
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts userId from JWT.
     */
    public Long getUserIdFromToken(String token) {
        Number userId = (Number) getClaims(token).get("userId");
        return userId != null ? userId.longValue() : null;
    }

    /**
     * Extracts role from JWT.
     */
    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    /**
     * Validates JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
