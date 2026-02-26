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
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.prography.backend.support.TestFixtures.attendance;
import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.cohortMember;
import static com.prography.backend.support.TestFixtures.createSessionRequest;
import static com.prography.backend.support.TestFixtures.member;
import static com.prography.backend.support.TestFixtures.qrCode;
import static com.prography.backend.support.TestFixtures.session;
import static com.prography.backend.support.TestFixtures.updateSessionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    @Mock
    private ClubSessionRepository clubSessionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private QrCodeRepository qrCodeRepository;

    @Mock
    private CurrentCohortProvider currentCohortProvider;

    @InjectMocks
    private AdminSessionService adminSessionService;

    @Test
    void 일정생성시_세션과_QR이_함께생성된다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        SessionRequestDTO.CreateSessionRequestDTO request =
                createSessionRequest("정기 모임", LocalDate.of(2026, 3, 1), "14:00", "강남");

        // when
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(clubSessionRepository.save(any(ClubSession.class))).thenAnswer(invocation -> { // 실제 DB 저장 없이 JPA의 save() 후 ID가 채워지는 동작을 시뮬레이션
            ClubSession saved = invocation.getArgument(0, ClubSession.class);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        when(qrCodeRepository.save(any(QrCode.class))).thenAnswer(invocation -> {
            QrCode saved = invocation.getArgument(0, QrCode.class);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        SessionResponseDTO.SessionResultDTO result = adminSessionService.createSession(request);

        // then
        ArgumentCaptor<QrCode> qrCaptor = ArgumentCaptor.forClass(QrCode.class);
        verify(qrCodeRepository).save(qrCaptor.capture());
        QrCode savedQr = qrCaptor.getValue();
        assertThat(savedQr.getHashValue()).isNotBlank();
        assertThat(savedQr.getExpiresAt()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(result.getQrActive()).isTrue();
        assertThat(result.getAttendanceSummary().getTotal()).isEqualTo(0);
    }

    @Test
    void 일정목록조회시_날짜범위가_잘못되면_INVALID_INPUT_예외() {

        assertThatThrownBy(() ->
                adminSessionService.getSessionList(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), null)
        )
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 일정목록조회시_출결요약과_QR활성여부를_반환한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession s1 = session(1L, cohort, "세션1", LocalDateTime.of(2026, 3, 1, 14, 0), "강남", SessionStatus.SCHEDULED);
        ClubSession s2 = session(2L, cohort, "세션2", LocalDateTime.of(2026, 3, 8, 14, 0), "강남", SessionStatus.CANCELLED);

        var member = member(10L, "user", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        var cohortMember = cohortMember(20L, cohort, member, null, null, 90_000, 0);
        List<Attendance> attendances = List.of(
                attendance(100L, s1, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.QR, Instant.now(), null, 0, null),
                attendance(101L, s1, member, cohortMember, null, AttendanceStatus.LATE, AttendanceSource.QR, Instant.now(), 7, 3_500, null),
                attendance(102L, s2, member, cohortMember, null, AttendanceStatus.ABSENT, AttendanceSource.ADMIN, null, null, 10_000, "결석")
        );
        QrCode activeQr = qrCode(200L, s1, "hash", Instant.now().plus(1, ChronoUnit.HOURS));

        // when
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(clubSessionRepository.findAllByCohortIdOrderByStartsAtAsc(2L)).thenReturn(List.of(s1, s2));
        when(attendanceRepository.findAllBySessionIdIn(List.of(1L, 2L))).thenReturn(attendances);
        when(qrCodeRepository.findActiveBySessionIdIn(anyList(), any(Instant.class))).thenReturn(List.of(activeQr));

        List<SessionResponseDTO.SessionResultDTO> result = adminSessionService.getSessionList(null, null, null);

        // then
        assertThat(result).hasSize(2);
        SessionResponseDTO.SessionResultDTO first = result.getFirst();
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getAttendanceSummary().getPresent()).isEqualTo(1);
        assertThat(first.getAttendanceSummary().getLate()).isEqualTo(1);
        assertThat(first.getAttendanceSummary().getTotal()).isEqualTo(2);
        assertThat(first.getQrActive()).isTrue();
    }

    @Test
    void 일정수정시_일정이없으면_SESSION_NOT_FOUND_예외() {

        when(clubSessionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminSessionService.updateSession(404L, updateSessionRequest("제목", null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void 일정수정시_취소된일정이면_SESSION_ALREADY_CANCELLED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession cancelled = session(1L, cohort, "취소됨", LocalDateTime.now(), "강남", SessionStatus.CANCELLED);

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        // then
        assertThatThrownBy(() -> adminSessionService.updateSession(1L, updateSessionRequest("변경", null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_ALREADY_CANCELLED);
    }

    @Test
    void 일정수정_성공() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "기존", LocalDateTime.of(2026, 3, 1, 14, 0), "강남", SessionStatus.SCHEDULED);
        SessionRequestDTO.UpdateSessionRequestDTO request =
                updateSessionRequest("변경", LocalDate.of(2026, 3, 2), "15:30", "잠실", SessionStatus.IN_PROGRESS);

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(clubSessionRepository.saveAndFlush(session)).thenReturn(session);
        when(attendanceRepository.findAllBySessionIdOrderByCheckedAtAsc(1L)).thenReturn(List.of());
        when(qrCodeRepository.findActiveBySessionId(eq(1L), any(Instant.class))).thenReturn(List.of());

        SessionResponseDTO.SessionResultDTO result = adminSessionService.updateSession(1L, request);

        // then
        assertThat(result.getTitle()).isEqualTo("변경");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(result.getTime().getHour()).isEqualTo(15);
        assertThat(result.getLocation()).isEqualTo("잠실");
        assertThat(result.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void 일정수정시_시간형식이_잘못되면_INVALID_INPUT_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "기존", LocalDateTime.of(2026, 3, 1, 14, 0), "강남", SessionStatus.SCHEDULED);

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // then
        assertThatThrownBy(() -> adminSessionService.updateSession(
                1L,
                updateSessionRequest(null, null, "25:00", null, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 일정삭제시_소프트삭제되고_QR이_비활성화된다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "정기모임", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        QrCode qr1 = qrCode(10L, session, "h1", Instant.now().plus(1, ChronoUnit.HOURS));
        QrCode qr2 = qrCode(11L, session, "h2", Instant.now().plus(2, ChronoUnit.HOURS));

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(qrCodeRepository.findActiveBySessionId(eq(1L), any(Instant.class))).thenReturn(List.of(qr1, qr2));
        when(clubSessionRepository.saveAndFlush(session)).thenReturn(session);
        when(attendanceRepository.findAllBySessionIdOrderByCheckedAtAsc(1L)).thenReturn(List.of());

        SessionResponseDTO.SessionResultDTO result = adminSessionService.deleteSession(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(result.getQrActive()).isFalse();
        assertThat(qr1.getRevokedAt()).isNotNull();
        assertThat(qr2.getRevokedAt()).isNotNull();
    }

    @Test
    void 일정삭제시_이미취소된일정이면_SESSION_ALREADY_CANCELLED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession cancelled = session(1L, cohort, "취소됨", LocalDateTime.now(), "강남", SessionStatus.CANCELLED);

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        // then
        assertThatThrownBy(() -> adminSessionService.deleteSession(1L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_ALREADY_CANCELLED);
    }

    @Test
    void QR생성_성공() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "정기모임", LocalDateTime.now(), "강남", SessionStatus.SCHEDULED);

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(qrCodeRepository.findActiveBySessionId(eq(1L), any(Instant.class))).thenReturn(List.of());
        when(qrCodeRepository.saveAndFlush(any(QrCode.class))).thenAnswer(invocation -> {
            QrCode saved = invocation.getArgument(0, QrCode.class);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        SessionResponseDTO.QrCodeResultDTO result = adminSessionService.createQrCode(1L);

        // then
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getHashValue()).isNotBlank();
        assertThat(result.getExpiresAt()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));
    }

    @Test
    void QR생성시_일정이없으면_SESSION_NOT_FOUND_예외() {

        when(clubSessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminSessionService.createQrCode(999L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void QR생성시_활성QR이있으면_QR_ALREADY_ACTIVE_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "정기모임", LocalDateTime.now(), "강남", SessionStatus.SCHEDULED);
        QrCode activeQr = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));

        // when
        when(clubSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(qrCodeRepository.findActiveBySessionId(eq(1L), any(Instant.class))).thenReturn(List.of(activeQr));

        // then
        assertThatThrownBy(() -> adminSessionService.createQrCode(1L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QR_ALREADY_ACTIVE);
    }
}
