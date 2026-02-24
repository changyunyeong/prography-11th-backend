package com.prography.backend.domain.deposit.dto;

import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.global.common.enums.DepositType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DepositResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositHistoryDTO {
        private Long id;
        private Long cohortMemberId;
        private String type;
        private int amount;
        private int balanceAfter;
        private Long attendanceId;
        private String description;
        private LocalDateTime createdAt;

        public static DepositHistoryDTO from(DepositHistory history) {
            int displayAmount = history.getAmount();
            if (history.getType() == DepositType.PENALTY) {
                displayAmount = -displayAmount;
            }

            return DepositHistoryDTO.builder()
                .id(history.getId())
                .cohortMemberId(history.getCohortMember().getId())
                .type(history.getType().name())
                .amount(displayAmount)
                .balanceAfter(history.getBalanceAfter())
                .attendanceId(history.getAttendance() != null ? history.getAttendance().getId() : null)
                .description(history.getDescription())
                .createdAt(history.getCreatedAt())
                .build();
        }
    }
}
