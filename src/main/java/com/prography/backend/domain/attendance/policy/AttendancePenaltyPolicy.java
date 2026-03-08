package com.prography.backend.domain.attendance.policy;

import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AttendancePenaltyPolicy {

    private static final int ABSENT_PENALTY = 10_000;
    private static final int LATE_PENALTY_PER_MINUTE = 500;
    private static final int MAX_LATE_PENALTY = 10_000;

    public Integer normalizeLateMinutes(AttendanceStatus status, Integer lateMinutes) {
        if (status != AttendanceStatus.LATE) {
            return null;
        }
        if (lateMinutes == null || lateMinutes < 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
        return lateMinutes;
    }

    public int calculatePenaltyAmount(AttendanceStatus status, Integer lateMinutes) {
        if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.EXCUSED) {
            return 0;
        }
        if (status == AttendanceStatus.ABSENT) {
            return ABSENT_PENALTY;
        }

        if (lateMinutes == null || lateMinutes < 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        long latePenalty = (long) lateMinutes * LATE_PENALTY_PER_MINUTE;
        return (int) Math.min(latePenalty, MAX_LATE_PENALTY);
    }
}
