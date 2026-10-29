package com.bankease.backend.repository;

import com.bankease.backend.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByUserEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(String userEmail, String purpose);
}
