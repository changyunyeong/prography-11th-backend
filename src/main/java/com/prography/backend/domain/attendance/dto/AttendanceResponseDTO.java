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
}
