package com.prography.backend.domain.session.service;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.attendance.repository.AttendanceRepository;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.qrcode.repository.QrCodeRepository;
import com.prography.backend.domain.session.dto.SessionRequestDTO;
import com.prography.backend.domain.session.dto.SessionResponseDTO;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.domain.session.repository.ClubSessionRepository;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SessionService {

    private static final DateTimeFormatter REQUEST_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final ClubSessionRepository clubSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final QrCodeRepository qrCodeRepository;
    private final CurrentCohortProvider currentCohortProvider;
    private final Clock clock;

    public SessionResponseDTO.SessionResultDTO createSession(SessionRequestDTO.CreateSessionRequestDTO request) {
        Cohort cohort = currentCohortProvider.getCurrentCohort();
        LocalTime parsedTime = parseRequestTime(request.getTime());
        LocalDateTime startAt = LocalDateTime.of(request.getDate(), parsedTime);

        ClubSession session = ClubSession.builder()
                .cohort(cohort)
                .title(request.getTitle())
                .startsAt(startAt)
                .location(request.getLocation())
                .status(SessionStatus.SCHEDULED)
                .build();
        session = clubSessionRepository.save(session);

        LocalDateTime now = LocalDateTime.now(clock);
        QrCode qrCode = QrCode.builder()
                .session(session)
                .hashValue(UUID.randomUUID().toString())
                .expiresAt(now.plusHours(24))
                .build();
        qrCodeRepository.save(qrCode);

        return SessionResponseDTO.SessionResultDTO.from(
                session,
                createEmptyAttendanceSummary(),
                true
        );
    }

    public List<SessionResponseDTO.SessionResultDTO> getSessionList(
            LocalDate dateFrom,
            LocalDate dateTo,
            SessionStatus status
    ) {
        validateDateRange(dateFrom, dateTo);

        Long cohortId = currentCohortProvider.getCurrentCohort().getId();
        List<ClubSession> sessions = clubSessionRepository.findAllByCohortIdOrderByStartsAtAsc(cohortId).stream()
                .filter(session -> matchesFilters(session, dateFrom, dateTo, status))
                .toList();

        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream()
                .map(ClubSession::getId)
                .toList();

        Map<Long, AttendanceSummaryAccumulator> summaryMap = initializeSummaryMap(sessionIds);
        List<Attendance> attendances = attendanceRepository.findAllBySessionIdIn(sessionIds);
        for (Attendance attendance : attendances) {
            Long sessionId = attendance.getSession().getId();
            AttendanceSummaryAccumulator summary = summaryMap.get(sessionId);
            if (summary != null) {
                summary.add(attendance.getStatus());
            }
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> activeQrSessionIds = qrCodeRepository.findActiveBySessionIdIn(sessionIds, now).stream()
                .map(QrCode::getSession)
                .map(ClubSession::getId)
                .collect(Collectors.toSet());

        return sessions.stream()
                .map(session -> SessionResponseDTO.SessionResultDTO.from(
                        session,
                        summaryMap.get(session.getId()).toDto(),
                        activeQrSessionIds.contains(session.getId())
                ))
                .toList();
    }

    public SessionResponseDTO.SessionResultDTO updateSession(
            Long sessionId,
            SessionRequestDTO.UpdateSessionRequestDTO request
    ) {
        ClubSession session = clubSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_CANCELLED);
        }

        LocalDateTime startAt = null;
        if (request.getDate() != null || request.getTime() != null) {
            LocalDate targetDate = request.getDate() != null
                    ? request.getDate()
                    : session.getStartsAt().toLocalDate();

            LocalTime targetTime = request.getTime() != null
                    ? parseRequestTime(request.getTime())
                    : session.getStartsAt().toLocalTime();

            startAt = LocalDateTime.of(targetDate, targetTime);
        }

        session.updateSession(
                request.getTitle(),
                startAt,
                request.getLocation(),
                request.getStatus()
        );
        session = clubSessionRepository.saveAndFlush(session);

        SessionResponseDTO.AttendanceSummaryDTO attendanceSummary = getAttendanceSummary(session.getId());
        boolean qrActive = isQrActive(session.getId());

        return SessionResponseDTO.SessionResultDTO.from(session, attendanceSummary, qrActive);
    }

    public SessionResponseDTO.SessionResultDTO deleteSession(Long sessionId) {
        ClubSession session = clubSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_CANCELLED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<QrCode> activeQrCodes = qrCodeRepository.findActiveBySessionId(sessionId, now);
        for (QrCode qrCode : activeQrCodes) {
            qrCode.revoke(now);
        }

        session.updateStatus(SessionStatus.CANCELLED);
        session = clubSessionRepository.saveAndFlush(session);

        SessionResponseDTO.AttendanceSummaryDTO attendanceSummary = getAttendanceSummary(session.getId());

        return SessionResponseDTO.SessionResultDTO.from(session, attendanceSummary, false);
    }

    private LocalTime parseRequestTime(String rawTime) {
        try {
            return LocalTime.parse(rawTime, REQUEST_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private SessionResponseDTO.AttendanceSummaryDTO createEmptyAttendanceSummary() {
        return SessionResponseDTO.AttendanceSummaryDTO.builder()
                .present(0)
                .absent(0)
                .late(0)
                .excused(0)
                .total(0)
                .build();
    }

    private SessionResponseDTO.AttendanceSummaryDTO getAttendanceSummary(Long sessionId) {
        List<Attendance> attendances = attendanceRepository.findAllBySessionIdOrderByCheckedAtAsc(sessionId);
        AttendanceSummaryAccumulator accumulator = new AttendanceSummaryAccumulator();
        for (Attendance attendance : attendances) {
            accumulator.add(attendance.getStatus());
        }
        return accumulator.toDto();
    }

    private boolean isQrActive(Long sessionId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return !qrCodeRepository.findActiveBySessionId(sessionId, now).isEmpty();
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean matchesFilters(ClubSession session, LocalDate dateFrom, LocalDate dateTo, SessionStatus status) {
        LocalDate sessionDate = session.getStartsAt().toLocalDate();

        if (dateFrom != null && sessionDate.isBefore(dateFrom)) {
            return false;
        }
        if (dateTo != null && sessionDate.isAfter(dateTo)) {
            return false;
        }
        if (status != null && session.getStatus() != status) {
            return false;
        }
        return true;
    }

    private Map<Long, AttendanceSummaryAccumulator> initializeSummaryMap(List<Long> sessionIds) {
        Map<Long, AttendanceSummaryAccumulator> summaryMap = new HashMap<>();
        for (Long sessionId : sessionIds) {
            summaryMap.put(sessionId, new AttendanceSummaryAccumulator());
        }
        return summaryMap;
    }

    private static final class AttendanceSummaryAccumulator {
        private int present;
        private int absent;
        private int late;
        private int excused;
        private int total;

        private void add(AttendanceStatus status) {
            if (status == AttendanceStatus.PRESENT) {
                present++;
            } else if (status == AttendanceStatus.ABSENT) {
                absent++;
            } else if (status == AttendanceStatus.LATE) {
                late++;
            } else if (status == AttendanceStatus.EXCUSED) {
                excused++;
            }
            total++;
        }

        private SessionResponseDTO.AttendanceSummaryDTO toDto() {
            return SessionResponseDTO.AttendanceSummaryDTO.builder()
                    .present(present)
                    .absent(absent)
                    .late(late)
                    .excused(excused)
                    .total(total)
                    .build();
        }
    }
}
