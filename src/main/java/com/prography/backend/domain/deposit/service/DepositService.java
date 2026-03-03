package com.prography.backend.domain.deposit.service;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.domain.deposit.repository.DepositHistoryRepository;
import com.prography.backend.global.common.enums.DepositType;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DepositService {

    private final DepositHistoryRepository depositHistoryRepository;

    public void initializeDeposit(CohortMember cohortMember, int amount, String description) {

        if (amount <= 0) {
            return;
        }

        int before = cohortMember.getDepositBalance();
        cohortMember.increaseDeposit(amount);
        int after = cohortMember.getDepositBalance();
        record(cohortMember, DepositType.INITIAL, amount, before, after, null, description);
    }

    public void applyPenalty(CohortMember cohortMember, int amount, Attendance attendance, String description) {
        if (amount <= 0) {
            return;
        }

        int before = cohortMember.getDepositBalance();
        if (before < amount) {
            throw new ApiException(ErrorCode.DEPOSIT_INSUFFICIENT);
        }

        cohortMember.decreaseDeposit(amount);
        int after = cohortMember.getDepositBalance();
        record(cohortMember, DepositType.PENALTY, amount, before, after, attendance, description);
    }

    public void applyRefund(CohortMember cohortMember, int amount, Attendance attendance, String description) {
        if (amount <= 0) {
            return;
        }

        int before = cohortMember.getDepositBalance();
        cohortMember.increaseDeposit(amount);
        int after = cohortMember.getDepositBalance();
        record(cohortMember, DepositType.REFUND, amount, before, after, attendance, description);
    }

    private void record(
        CohortMember cohortMember,
        DepositType type,
        int amount,
        int balanceBefore,
        int balanceAfter,
        Attendance attendance,
        String description
    ) {
        DepositHistory history = DepositHistory.builder()
            .cohortMember(cohortMember)
            .type(type)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceAfter)
            .attendance(attendance)
            .description(description)
            .build();

        depositHistoryRepository.save(history);
    }

}
