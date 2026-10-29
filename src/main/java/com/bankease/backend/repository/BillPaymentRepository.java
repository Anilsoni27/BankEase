package com.bankease.backend.repository;

import com.bankease.backend.entity.BillPayment;
import com.bankease.backend.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<BillPayment> findByStatus(BillStatus status);
}
