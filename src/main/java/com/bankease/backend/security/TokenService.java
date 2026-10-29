package com.bankease.backend.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenService manages lightweight in-memory authentication tokens.
 *
 * As specified for this MVP:
 * - A random UUID token is generated on successful login or registration.
 * - Stored in a thread-safe ConcurrentHashMap mapping "UUID token" -> "userId".
 * - Avoids heavyweight JWT/Spring Security configurations for simplicity,
 *   while maintaining clean token-based authorization via HTTP headers.
 */
@Service
public class TokenService {

    /**
     * Thread-safe in-memory map storing active user sessions.
     * Key: session token (UUID string)
     * Value: database ID of the authenticated user
     */
    private final Map<String, Long> tokenToUserIdMap = new ConcurrentHashMap<>();

    /**
     * Issues a new random UUID token for a user and stores it in memory.
     */
    public String generateToken(Long userId) {
        String token = UUID.randomUUID().toString();
        tokenToUserIdMap.put(token, userId);
        return token;
    }

    /**
     * Retrieves the user ID associated with the provided session token.
     * Returns null if the token does not exist or has been invalidated.
     */
    public Long getUserIdByToken(String token) {
        if (token == null) {
            return null;
        }
        return tokenToUserIdMap.get(token);
    }

    /**
     * Invalidates (logs out) an active session token.
     */
    public void invalidateToken(String token) {
        if (token != null) {
            tokenToUserIdMap.remove(token);
        }
    }

    /**
     * Verifies if a given token is present in the active session store.
     */
    public boolean isValid(String token) {
        return token != null && tokenToUserIdMap.containsKey(token);
    }
}

