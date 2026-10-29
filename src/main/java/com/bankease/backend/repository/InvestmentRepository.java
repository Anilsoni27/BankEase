package com.bankease.backend.repository;

import com.bankease.backend.entity.Investment;
import com.bankease.backend.entity.InvestmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Investment> findByStatus(InvestmentStatus status);
}

