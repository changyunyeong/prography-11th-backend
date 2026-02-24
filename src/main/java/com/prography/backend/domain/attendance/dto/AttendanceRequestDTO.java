package com.prography.backend.domain.attendance.dto;

import com.prography.backend.global.common.enums.AttendanceStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class AttendanceRequestDTO {

    @Getter
    public static class RegisterAttendanceRequestDTO {
        @NotNull
        private Long sessionId;
        @NotNull
        private Long memberId;
        @NotNull
        private AttendanceStatus status;
        @Min(0)
        private Integer lateMinutes;
        private String reason;
    }

    @Getter
    public static class UpdateAttendanceRequestDTO {
        @NotNull
        private AttendanceStatus status;
        @Min(0)
        private Integer lateMinutes;
        private String reason;
    }
}
