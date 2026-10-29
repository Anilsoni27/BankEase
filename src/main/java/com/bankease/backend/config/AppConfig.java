package com.bankease.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * AppConfig defines Spring beans needed throughout the BankEase application.
 *
 * Annotations explained:
 * - @Configuration: Designates this class as a source of bean definitions for the
 *   Spring ApplicationContext.
 */
@Configuration
public class AppConfig {

    /**
     * @Bean: Registers BCryptPasswordEncoder into the Spring container.
     * BCrypt is a strong, salted cryptographic hash function designed specifically
     * for securely hashing user passwords before database storage.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

