package com.prography.backend.domain.attendance.service;

import com.prography.backend.domain.attendance.dto.AttendanceRequestDTO;
import com.prography.backend.domain.attendance.dto.AttendanceResponseDTO;
import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.attendance.repository.AttendanceRepository;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.deposit.service.DepositService;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.qrcode.repository.QrCodeRepository;
import com.prography.backend.domain.session.entity.ClubSession;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.prography.backend.support.TestFixtures.attendance;
import static com.prography.backend.support.TestFixtures.checkAttendanceRequest;
import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.cohortMember;
import static com.prography.backend.support.TestFixtures.member;
import static com.prography.backend.support.TestFixtures.qrCode;
import static com.prography.backend.support.TestFixtures.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private QrCodeRepository qrCodeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private DepositService depositService;

    @Mock
    private CurrentCohortProvider currentCohortProvider;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void QR출석체크시_QR이유효하지않으면_QR_INVALID_예외() {
        // given
        AttendanceRequestDTO.CheckAttendanceRequestDTO request = checkAttendanceRequest("missing", 1L);

        // when
        when(qrCodeRepository.findByHashValue("missing")).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QR_INVALID);
    }

    @Test
    void QR출석체크시_QR만료면_QR_EXPIRED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().minus(1, ChronoUnit.SECONDS));

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QR_EXPIRED);
    }

    @Test
    void QR출석체크시_일정진행중아니면_SESSION_NOT_IN_PROGRESS_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.SCHEDULED);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_IN_PROGRESS);
    }

    @Test
    void QR출석체크시_회원없으면_MEMBER_NOT_FOUND_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void QR출석체크시_탈퇴회원이면_MEMBER_WITHDRAWN_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        Member withdrawn = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.WITHDRAWN);

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
    }

    @Test
    void QR출석체크시_이미출결있으면_ATTENDANCE_ALREADY_CHECKED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(true);

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
    }

    @Test
    void QR출석체크시_기수회원없으면_COHORT_MEMBER_NOT_FOUND_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COHORT_MEMBER_NOT_FOUND);
    }

    @Test
    void QR출석체크시_정시출석이면_PRESENT로_저장된다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        LocalDateTime seoulFuture = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusHours(2);
        ClubSession session = session(1L, cohort, "세션", seoulFuture, "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(20L, cohort, member, null, null, 100_000, 0);

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance saved = invocation.getArgument(0, Attendance.class);
            ReflectionTestUtils.setField(saved, "id", 999L);
            return saved;
        });

        AttendanceResponseDTO.AttendanceResultDTO result = attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L));

        // then
        assertThat(result.getId()).isEqualTo(999L);
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getLateMinutes()).isNull();
        assertThat(result.getPenaltyAmount()).isEqualTo(0);

        ArgumentCaptor<Attendance> attendanceCaptor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository).saveAndFlush(attendanceCaptor.capture());
        assertThat(attendanceCaptor.getValue().getSource()).isEqualTo(AttendanceSource.QR);
        verify(depositService).applyPenalty(eq(cohortMember), eq(0), any(Attendance.class), anyString());
    }

    @Test
    void QR출석체크시_지각패널티는_최대10000원으로_제한된다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        LocalDateTime seoulPast = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(25);
        ClubSession session = session(1L, cohort, "세션", seoulPast, "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(20L, cohort, member, null, null, 100_000, 0);

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance saved = invocation.getArgument(0, Attendance.class);
            ReflectionTestUtils.setField(saved, "id", 1000L);
            return saved;
        });

        AttendanceResponseDTO.AttendanceResultDTO result = attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L));

        // then
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(result.getLateMinutes()).isNotNull().isPositive();
        assertThat(result.getPenaltyAmount()).isEqualTo(10_000);
        verify(depositService).applyPenalty(eq(cohortMember), eq(10_000), any(Attendance.class), anyString());
    }

    @Test
    void QR출석체크시_보증금부족예외를_전파한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        LocalDateTime seoulPast = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1);
        ClubSession session = session(1L, cohort, "세션", seoulPast, "강남", SessionStatus.IN_PROGRESS);
        QrCode qrCode = qrCode(10L, session, "hash", Instant.now().plus(1, ChronoUnit.HOURS));
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(20L, cohort, member, null, null, 1_000, 0);

        // when
        when(qrCodeRepository.findByHashValue("hash")).thenReturn(Optional.of(qrCode));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ApiException(ErrorCode.DEPOSIT_INSUFFICIENT))
                .when(depositService).applyPenalty(eq(cohortMember), anyInt(), any(Attendance.class), anyString());

        // then
        assertThatThrownBy(() -> attendanceService.checkAttendance(checkAttendanceRequest("hash", 1L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEPOSIT_INSUFFICIENT);
    }

    @Test
    void 내출결조회_성공() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(2L, cohort, member, null, null, 100_000, 0);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Attendance attendance = attendance(
                1L, session, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.QR, Instant.now(), null, 0, null
        );

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.findAllByMemberIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(attendance));

        List<AttendanceResponseDTO.AttendanceHistoryDTO> result = attendanceService.getAttendances(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSessionId()).isEqualTo(1L);
    }

    @Test
    void 내출결조회시_회원없으면_MEMBER_NOT_FOUND_예외() {

        when(memberRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.getAttendances(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
