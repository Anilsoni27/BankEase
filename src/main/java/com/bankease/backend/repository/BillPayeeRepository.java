package com.bankease.backend.repository;

import com.bankease.backend.entity.BillCategory;
import com.bankease.backend.entity.BillPayee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPayeeRepository extends JpaRepository<BillPayee, Long> {
    List<BillPayee> findByUserId(Long userId);
    List<BillPayee> findByUserIdAndCategory(Long userId, BillCategory category);
}
