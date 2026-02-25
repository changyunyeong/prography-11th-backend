package com.prography.backend.domain.session.dto;

import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.global.common.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SessionResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionResultDTO {
        private Long id;
        private Long cohortId;
        private String title;
        private LocalDate date;
        private LocalTime time;
        private String location;
        private SessionStatus status;
        private AttendanceSummaryDTO attendanceSummary;
        private Boolean qrActive;
        private Instant createdAt;
        private Instant updatedAt;

        public static SessionResultDTO from(
                ClubSession session,
                AttendanceSummaryDTO attendanceSummary,
                boolean qrActive
        ) {
            LocalDateTime startsAt = session.getStartsAt();
            return SessionResultDTO.builder()
                    .id(session.getId())
                    .cohortId(session.getCohort().getId())
                    .title(session.getTitle())
                    .date(startsAt.toLocalDate())
                    .time(startsAt.toLocalTime())
                    .location(session.getLocation())
                    .status(session.getStatus())
                    .attendanceSummary(attendanceSummary)
                    .qrActive(qrActive)
                    .createdAt(session.getCreatedAt())
                    .updatedAt(session.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummaryDTO {
        private int present;
        private int absent;
        private int late;
        private int excused;
        private int total;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrCodeResultDTO {
        private Long id;
        private Long sessionId;
        private String hashValue;
        private Instant createdAt;
        private Instant expiresAt;

        public static QrCodeResultDTO from(QrCode qrCode) {
            return QrCodeResultDTO.builder()
                    .id(qrCode.getId())
                    .sessionId(qrCode.getSession().getId())
                    .hashValue(qrCode.getHashValue())
                    .createdAt(qrCode.getCreatedAt())
                    .expiresAt(qrCode.getExpiresAt())
                    .build();
        }
    }
}
