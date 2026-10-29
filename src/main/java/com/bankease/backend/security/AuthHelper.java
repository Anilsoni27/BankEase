package com.bankease.backend.security;

import com.bankease.backend.entity.Role;
import com.bankease.backend.entity.User;
import com.bankease.backend.exception.ResourceNotFoundException;
import com.bankease.backend.exception.UnauthorizedException;
import com.bankease.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * AuthHelper inspects incoming HTTP Authorization headers, validates session tokens,
 * and extracts the authenticated User entity from the database.
 *
 * Annotations explained:
 * - @Component: Marks this class as a generic Spring-managed bean that can be
 *   injected into controllers or services.
 */
@Component
public class AuthHelper {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AuthHelper(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    /**
     * Extracts token from "Authorization" header, looks up the session,
     * and returns the authenticated User entity.
     *
     * Accepts either:
     * - "Bearer <uuid-token>"
     * - "<uuid-token>"
     */
    public User authenticate(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new UnauthorizedException("Missing Authorization header. Please log in.");
        }

        String token = authHeader.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        Long userId = tokenService.getUserIdByToken(token);
        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired session token. Please log in again.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user account no longer exists."));
    }

    /**
     * Enforces that the authenticated user possesses the ADMIN role.
     */
    public void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Access denied: Administrator privileges required.");
        }
    }
}

