package com.bankease.backend.repository;

import com.bankease.backend.entity.Loan;
import com.bankease.backend.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserIdOrderByAppliedAtDesc(Long userId);
    List<Loan> findByStatusOrderByAppliedAtDesc(LoanStatus status);
    List<Loan> findAllByOrderByAppliedAtDesc();
}

