package com.bankease.backend.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenService bridges JWT token issuance with in-memory active session tracking.
 */
@Service
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, Long> tokenToUserIdMap = new ConcurrentHashMap<>();

    public TokenService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Issues a signed JWT token and stores the session mapping in memory.
     */
    public String generateToken(Long userId, String email, String role) {
        String token = jwtTokenProvider.generateToken(userId, email, role);
        tokenToUserIdMap.put(token, userId);
        return token;
    }

    /**
     * Overload for backward compatibility.
     */
    public String generateToken(Long userId) {
        return generateToken(userId, "user" + userId + "@bankease.com", "CUSTOMER");
    }

    /**
     * Retrieves the user ID associated with the token.
     */
    public Long getUserIdByToken(String token) {
        if (token == null) return null;

        // Try extracting from JWT first
        if (jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUserIdFromToken(token);
        }

        // Fallback to in-memory store
        return tokenToUserIdMap.get(token);
    }

    /**
     * Invalidates an active token.
     */
    public void invalidateToken(String token) {
        if (token != null) {
            tokenToUserIdMap.remove(token);
        }
    }

    /**
     * Checks if token is valid.
     */
    public boolean isValid(String token) {
        return token != null && (jwtTokenProvider.validateToken(token) || tokenToUserIdMap.containsKey(token));
    }
}
