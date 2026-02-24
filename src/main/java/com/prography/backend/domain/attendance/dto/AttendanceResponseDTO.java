package com.prography.backend.domain.attendance.dto;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.global.common.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AttendanceResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceResultDTO {
        private Long id;
        private Long sessionId;
        private Long memberId;
        private AttendanceStatus status;
        private Integer lateMinutes;
        private Integer penaltyAmount;
        private String reason;
        private LocalDateTime checkedInAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static AttendanceResultDTO from(Attendance attendance) {
            return AttendanceResultDTO.builder()
                    .id(attendance.getId())
                    .sessionId(attendance.getSession().getId())
                    .memberId(attendance.getMember().getId())
                    .status(attendance.getStatus())
                    .lateMinutes(attendance.getLatenessMinutes())
                    .penaltyAmount(attendance.getPenaltyAmount())
                    .reason(attendance.getReason())
                    .checkedInAt(attendance.getCheckedAt())
                    .createdAt(attendance.getCreatedAt())
                    .updatedAt(attendance.getUpdatedAt())
                    .build();
        }

    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionAttendanceSummaryDTO {
        private Long memberId;
        private String memberName;
        private Integer present;
        private Integer absent;
        private Integer late;
        private Integer excused;
        private Integer totalPenalty;
        private Integer deposit;

        public static SessionAttendanceSummaryDTO from(
                Long memberId,
                String memberName,
                int present,
                int absent,
                int late,
                int excused,
                int totalPenalty,
                int deposit
        ) {
            return SessionAttendanceSummaryDTO.builder()
                    .memberId(memberId)
                    .memberName(memberName)
                    .present(present)
                    .absent(absent)
                    .late(late)
                    .excused(excused)
                    .totalPenalty(totalPenalty)
                    .deposit(deposit)
                    .build();
        }
    }
}
