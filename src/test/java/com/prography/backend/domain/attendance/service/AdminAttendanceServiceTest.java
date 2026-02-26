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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.prography.backend.support.TestFixtures.attendance;
import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.cohortMember;
import static com.prography.backend.support.TestFixtures.member;
import static com.prography.backend.support.TestFixtures.registerAttendanceRequest;
import static com.prography.backend.support.TestFixtures.session;
import static com.prography.backend.support.TestFixtures.updateAttendanceRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClubSessionRepository sessionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private CurrentCohortProvider currentCohortProvider;

    @Mock
    private DepositService depositService;

    @InjectMocks
    private AdminAttendanceService adminAttendanceService;

    @Test
    void 출결등록시_일정없으면_SESSION_NOT_FOUND_예외() {

        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.PRESENT, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void 출결등록시_회원없으면_MEMBER_NOT_FOUND_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.PRESENT, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 출결등록시_이미출결존재하면_ATTENDANCE_ALREADY_CHECKED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(true);

        // then
        assertThatThrownBy(() -> adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.PRESENT, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
    }

    @Test
    void 출결등록시_기수회원없으면_COHORT_MEMBER_NOT_FOUND_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.PRESENT, null, null)
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COHORT_MEMBER_NOT_FOUND);
    }

    @Test
    void 출결등록시_공결한도초과면_EXCUSE_LIMIT_EXCEEDED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 90_000, 3);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));

        // then
        assertThatThrownBy(() -> adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.EXCUSED, null, "병가")
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
    }

    @Test
    void 출결등록시_LATE인데_지각분없으면_INVALID_INPUT_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 90_000, 0);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));

        // then
        assertThatThrownBy(() -> adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.LATE, null, "지각")
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 출결등록시_ABSENT는_패널티를_적용한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 100_000, 0);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance saved = invocation.getArgument(0, Attendance.class);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        AttendanceResponseDTO.AttendanceResultDTO result = adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.ABSENT, null, "결석")
        );

        // then
        assertThat(result.getPenaltyAmount()).isEqualTo(10_000);
        verify(depositService).applyPenalty(eq(cohortMember), eq(10_000), any(Attendance.class), anyString());
    }

    @Test
    void 출결등록시_EXCUSED는_공결횟수를_증가시킨다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 100_000, 1);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.existsBySessionIdAndMemberId(1L, 1L)).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponseDTO.AttendanceResultDTO result = adminAttendanceService.registerAttendance(
                registerAttendanceRequest(1L, 1L, AttendanceStatus.EXCUSED, null, "병가")
        );

        // then
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(cohortMember.getExcuseCount()).isEqualTo(2);
        verify(depositService).applyPenalty(eq(cohortMember), eq(0), any(Attendance.class), anyString());
    }

    @Test
    void 출결수정시_출결없으면_ATTENDANCE_NOT_FOUND_예외() {

        when(attendanceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.updateAttendance(
                updateAttendanceRequest(AttendanceStatus.PRESENT, null, null), 1L
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_NOT_FOUND);
    }

    @Test
    void 출결수정시_기수회원없으면_COHORT_MEMBER_NOT_FOUND_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Attendance attendance = attendance(
                100L, session, member, cohortMember(11L, cohort, member, null, null, 90_000, 0),
                null, AttendanceStatus.PRESENT, AttendanceSource.ADMIN, null, null, 0, null
        );

        // when
        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminAttendanceService.updateAttendance(
                updateAttendanceRequest(AttendanceStatus.ABSENT, null, "결석"), 100L
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COHORT_MEMBER_NOT_FOUND);
    }

    @Test
    void 출결수정시_EXCUSED전환_한도초과면_EXCUSE_LIMIT_EXCEEDED_예외() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        CohortMember cohortMember = cohortMember(11L, cohort, member, null, null, 90_000, 3);
        Attendance attendance = attendance(
                100L, session, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.ADMIN, null, null, 0, null
        );

        // when
        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));

        // then
        assertThatThrownBy(() -> adminAttendanceService.updateAttendance(
                updateAttendanceRequest(AttendanceStatus.EXCUSED, null, "병가"), 100L
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
    }

    @Test
    void 출결수정시_패널티증가면_applyPenalty를_호출한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        CohortMember cohortMember = cohortMember(11L, cohort, member, null, null, 90_000, 0);
        Attendance attendance = attendance(
                100L, session, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.ADMIN, null, null, 0, null
        );

        // when
        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(attendance)).thenReturn(attendance);

        AttendanceResponseDTO.AttendanceResultDTO result = adminAttendanceService.updateAttendance(
                updateAttendanceRequest(AttendanceStatus.LATE, 10, "지각"), 100L
        );

        // then
        assertThat(result.getPenaltyAmount()).isEqualTo(5_000);
        verify(depositService).applyPenalty(eq(cohortMember), eq(5_000), eq(attendance), anyString());
    }

    @Test
    void 출결수정시_패널티감소면_applyRefund를_호출한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        CohortMember cohortMember = cohortMember(11L, cohort, member, null, null, 90_000, 1);
        Attendance attendance = attendance(
                100L, session, member, cohortMember, null, AttendanceStatus.ABSENT, AttendanceSource.ADMIN, null, null, 10_000, "결석"
        );

        // when
        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(attendance)).thenReturn(attendance);

        AttendanceResponseDTO.AttendanceResultDTO result = adminAttendanceService.updateAttendance(
                updateAttendanceRequest(AttendanceStatus.EXCUSED, null, "병가"), 100L
        );

        // then
        assertThat(result.getPenaltyAmount()).isEqualTo(0);
        assertThat(cohortMember.getExcuseCount()).isEqualTo(2);
        verify(depositService).applyRefund(eq(cohortMember), eq(10_000), eq(attendance), anyString());
    }

    @Test
    void 출결수정시_패널티동일하면_보증금조정이없다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        CohortMember cohortMember = cohortMember(11L, cohort, member, null, null, 90_000, 0);
        Attendance attendance = attendance(
                100L, session, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.ADMIN, null, null, 0, null
        );

        // when
        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(attendance)).thenReturn(attendance);

        adminAttendanceService.updateAttendance(updateAttendanceRequest(AttendanceStatus.EXCUSED, null, "병가"), 100L);

        // then
        verify(depositService, never()).applyPenalty(any(), anyInt(), any(), anyString());
        verify(depositService, never()).applyRefund(any(), anyInt(), any(), anyString());
    }

    @Test
    void 출결수정시_EXCUSED에서_PRESENT로변경하면_공결횟수감소() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        CohortMember cohortMember = cohortMember(11L, cohort, member, null, null, 90_000, 2);
        Attendance attendance = attendance(
                100L, session, member, cohortMember, null, AttendanceStatus.EXCUSED, AttendanceSource.ADMIN, null, null, 0, null
        );

        // when
        when(attendanceRepository.findById(100L)).thenReturn(Optional.of(attendance));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.saveAndFlush(attendance)).thenReturn(attendance);

        adminAttendanceService.updateAttendance(updateAttendanceRequest(AttendanceStatus.PRESENT, null, null), 100L);

        // then
        assertThat(cohortMember.getExcuseCount()).isEqualTo(1);
    }

    @Test
    void 일정별출결요약조회시_일정없으면_SESSION_NOT_FOUND_예외() {

        when(sessionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.getSessionAttendanceSummary(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void 일정별출결요약조회시_현재기수일정이아니면_SESSION_NOT_FOUND_예외() {
        // given
        Cohort cohort10 = cohort(1L, 10, "10기", false);
        Cohort cohort11 = cohort(2L, 11, "11기", true);
        ClubSession session = session(100L, cohort10, "다른 기수 세션", LocalDateTime.now(), "강남", SessionStatus.SCHEDULED);

        // when
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort11);

        // then
        assertThatThrownBy(() -> adminAttendanceService.getSessionAttendanceSummary(100L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void 일정별출결요약조회시_전체출결집계후_회원ID순정렬한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "정기모임", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member1 = member(1L, "u1", "pw", "가", "0101", MemberRole.MEMBER, MemberStatus.ACTIVE);
        Member member2 = member(2L, "u2", "pw", "나", "0102", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cm2 = cohortMember(20L, cohort, member2, null, null, 80_000, 0);
        CohortMember cm1 = cohortMember(10L, cohort, member1, null, null, 90_000, 0);

        Attendance a1 = attendance(1L, session, member1, cm1, null, AttendanceStatus.PRESENT, AttendanceSource.QR, Instant.now(), null, 0, null);
        Attendance a2 = attendance(2L, session, member1, cm1, null, AttendanceStatus.LATE, AttendanceSource.ADMIN, null, 10, 5_000, "지각");
        Attendance a3 = attendance(3L, session, member2, cm2, null, AttendanceStatus.ABSENT, AttendanceSource.ADMIN, null, null, 10_000, "결석");

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findAllByCohortIdOrderByIdAsc(2L)).thenReturn(List.of(cm2, cm1));
        when(attendanceRepository.findAllByCohortMemberIdIn(List.of(20L, 10L))).thenReturn(List.of(a1, a2, a3));

        List<AttendanceResponseDTO.SessionAttendanceSummaryDTO> result =
                adminAttendanceService.getSessionAttendanceSummary(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMemberId()).isEqualTo(1L);
        assertThat(result.get(0).getPresent()).isEqualTo(1);
        assertThat(result.get(0).getLate()).isEqualTo(1);
        assertThat(result.get(0).getTotalPenalty()).isEqualTo(5_000);
        assertThat(result.get(1).getMemberId()).isEqualTo(2L);
        assertThat(result.get(1).getAbsent()).isEqualTo(1);
        assertThat(result.get(1).getDeposit()).isEqualTo(80_000);
    }

    @Test
    void 회원출결상세조회시_회원없으면_MEMBER_NOT_FOUND_예외() {

        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.getAttendanceDetail(999L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 회원출결상세조회_성공() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 90_000, 1);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Attendance attendance = attendance(
                1L, session, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.QR, Instant.now(), null, 0, null
        );

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));
        when(attendanceRepository.findAllByMemberIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(attendance));

        AttendanceResponseDTO.AttendanceDetailDTO result = adminAttendanceService.getAttendanceDetail(1L);

        // then
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getDeposit()).isEqualTo(90_000);
        assertThat(result.getAttendances()).hasSize(1);
    }

    @Test
    void 일정별출결목록조회시_일정없으면_SESSION_NOT_FOUND_예외() {

        when(sessionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAttendanceService.getSessionAttendanceDetail(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void 일정별출결목록조회시_현재기수일정이아니면_SESSION_NOT_FOUND_예외() {
        // given
        Cohort cohort10 = cohort(1L, 10, "10기", false);
        Cohort cohort11 = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort10, "세션", LocalDateTime.now(), "강남", SessionStatus.SCHEDULED);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort11);

        // then
        assertThatThrownBy(() -> adminAttendanceService.getSessionAttendanceDetail(1L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    void 일정별출결목록조회_성공() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "세션", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);
        Member member = member(1L, "u1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, cohort, member, null, null, 90_000, 0);
        Attendance attendance = attendance(
                1L, session, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.QR, Instant.now(), null, 0, null
        );

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(attendanceRepository.findAllBySessionIdOrderByCheckedAtAsc(1L)).thenReturn(List.of(attendance));

        // then
        AttendanceResponseDTO.SessionAttendanceDetailDTO result = adminAttendanceService.getSessionAttendanceDetail(1L);

        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getAttendances()).hasSize(1);
    }

    @Test
    void 일정별출결요약조회시_기수회원이없으면_빈목록반환() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        ClubSession session = session(1L, cohort, "정기모임", LocalDateTime.now(), "강남", SessionStatus.IN_PROGRESS);

        // when
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findAllByCohortIdOrderByIdAsc(2L)).thenReturn(List.of());

        List<AttendanceResponseDTO.SessionAttendanceSummaryDTO> result =
                adminAttendanceService.getSessionAttendanceSummary(1L);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(depositService);
    }
}
