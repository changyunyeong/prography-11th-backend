package com.prography.backend.domain.attendance.dto;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.global.common.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

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
        private Instant checkedInAt;
        private Instant createdAt;
        private Instant updatedAt;

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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceDetailDTO {
        private Long memberId;
        private String memberName;
        private Integer generation;
        private String partName;
        private String teamName;
        private Integer deposit;
        private Integer excuseCount;
        private List<AttendanceResultDTO> attendances;

        public static AttendanceDetailDTO from(
                Member member,
                CohortMember cohortMember,
                List<Attendance> attendances
        ) {
            return AttendanceDetailDTO.builder()
                    .memberId(member.getId())
                    .memberName(member.getName())
                    .generation(cohortMember != null ? cohortMember.getCohort().getGeneration() : null)
                    .partName(cohortMember != null && cohortMember.getPart() != null ? cohortMember.getPart().getType().name() : null)
                    .teamName(cohortMember != null && cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null)
                    .deposit(cohortMember != null ? cohortMember.getDepositBalance() : null)
                    .excuseCount(cohortMember != null ? cohortMember.getExcuseCount() : null)
                    .attendances(attendances == null ? List.of() : attendances.stream()
                            .map(AttendanceResultDTO::from)
                            .toList())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionAttendanceDetailDTO {
        private Long sessionId;
        private String sessionTitle;
        private List<AttendanceResultDTO> attendances;

        public static SessionAttendanceDetailDTO from(ClubSession session, List<Attendance> attendances) {
            return SessionAttendanceDetailDTO.builder()
                    .sessionId(session.getId())
                    .sessionTitle(session.getTitle())
                    .attendances((attendances == null ? List.<Attendance>of() : attendances).stream()
                            .map(AttendanceResultDTO::from)
                            .toList())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceHistoryDTO {
        private Long id;
        private Long sessionId;
        private String sessionTitle;
        private AttendanceStatus status;
        private Integer lateMinutes;
        private Integer penaltyAmount;
        private String reason;
        private Instant checkedInAt;
        private Instant createdAt;

        public static AttendanceHistoryDTO from(Attendance attendance) {
            return AttendanceHistoryDTO.builder()
                    .id(attendance.getId())
                    .sessionId(attendance.getSession().getId())
                    .sessionTitle(attendance.getSession().getTitle())
                    .status(attendance.getStatus())
                    .lateMinutes(attendance.getLatenessMinutes())
                    .penaltyAmount(attendance.getPenaltyAmount())
                    .reason(attendance.getReason())
                    .checkedInAt(attendance.getCheckedAt())
                    .createdAt(attendance.getCreatedAt())
                    .build();
        }
    }
}
