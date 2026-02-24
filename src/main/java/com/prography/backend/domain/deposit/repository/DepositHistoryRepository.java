package com.prography.backend.domain.deposit.repository;

import com.prography.backend.domain.deposit.entity.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
    List<DepositHistory> findAllByCohortMemberIdOrderByCreatedAtDesc(Long cohortMemberId);
}
