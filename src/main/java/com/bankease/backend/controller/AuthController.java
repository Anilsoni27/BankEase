package com.bankease.backend.controller;

import com.bankease.backend.dto.ApiResponse;
import com.bankease.backend.dto.AuthResponse;
import com.bankease.backend.dto.LoginRequest;
import com.bankease.backend.dto.RegisterRequest;
import com.bankease.backend.entity.User;
import com.bankease.backend.security.AuthHelper;
import com.bankease.backend.service.AccountService;
import com.bankease.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController exposes REST endpoints for customer registration, login, and session validation.
 *
 * Annotations explained:
 * - @RestController: Combines @Controller and @ResponseBody, meaning every handler method returns
 *   data serialized directly to JSON instead of rendering an HTML view template.
 * - @RequestMapping("/api/auth"): Base URL route prefix for all endpoints in this controller.
 * - @CrossOrigin: Allows browsers to make cross-origin AJAX requests during development.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthHelper authHelper;
    private final AccountService accountService;

    public AuthController(AuthService authService, AuthHelper authHelper, AccountService accountService) {
        this.authService = authService;
        this.authHelper = authHelper;
        this.accountService = accountService;
    }

    /**
     * Feature 1: Register endpoint
     *
     * Annotations explained:
     * - @PostMapping("/register"): Maps HTTP POST requests to /api/auth/register.
     * - @Valid: Triggers Jakarta Bean Validation on RegisterRequest fields (@NotBlank, @Email, @Size).
     *   If validation fails, Spring throws MethodArgumentNotValidException caught by GlobalExceptionHandler.
     * - @RequestBody: Deserializes incoming JSON HTTP request body into a Java RegisterRequest object.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success("Registration successful! Default SAVINGS account opened with ₹10,000 demo credit.", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Feature 2: Login endpoint
     * Verifies user credentials and returns session token with user profile info.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }

    /**
     * Logout endpoint
     * Invalidates the token in memory.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", "Session ended."));
    }

    /**
     * Profile verification endpoint (/api/auth/me)
     * Reads "Authorization" header and returns current user profile and accounts.
     *
     * - @RequestHeader("Authorization"): Reads the value of the HTTP "Authorization" header.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        User user = authHelper.authenticate(authHeader);
        AuthResponse response = new AuthResponse(
                authHeader.replace("Bearer ", "").trim(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                accountService.getAccountsByUser(user)
        );
        return ResponseEntity.ok(ApiResponse.success("User session verified.", response));
    }
}

