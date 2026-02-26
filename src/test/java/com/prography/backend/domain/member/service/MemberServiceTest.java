package com.prography.backend.domain.member.service;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.attendance.repository.AttendanceRepository;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.PartType;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.prography.backend.support.TestFixtures.attendance;
import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.cohortMember;
import static com.prography.backend.support.TestFixtures.loginRequest;
import static com.prography.backend.support.TestFixtures.member;
import static com.prography.backend.support.TestFixtures.part;
import static com.prography.backend.support.TestFixtures.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private CurrentCohortProvider currentCohortProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 로그인_성공() {
        // given
        Member member = member(2L, "user1", "encoded", "홍길동", "010-1111-2222", MemberRole.MEMBER, MemberStatus.ACTIVE); // 1L은 관리자
        MemberRequestDTO.LoginRequestDTO request = loginRequest("user1", "user1234");

        when(memberRepository.findByLoginId("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("user1234", "encoded")).thenReturn(true);

        // when
        MemberResponseDTO.MemberResultDTO result = memberService.login(request);

        // then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getLoginId()).isEqualTo("user1");
        assertThat(result.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 로그인시_회원없으면_LOGIN_FAILED_예외() {
        // given
        MemberRequestDTO.LoginRequestDTO request = loginRequest("missing", "pw");

        // when
        when(memberRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void 로그인시_비밀번호불일치면_LOGIN_FAILED_예외() {
        // given
        Member member = member(2L, "user1", "encoded", "홍길동", "010-1111-2222", MemberRole.MEMBER, MemberStatus.ACTIVE);
        MemberRequestDTO.LoginRequestDTO request = loginRequest("user1", "wrong");

        // when
        when(memberRepository.findByLoginId("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        // then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void 로그인시_탈퇴회원이면_MEMBER_WITHDRAWN_예외() {
        // given
        Member member = member(2L, "user1", "encoded", "홍길동", "010-1111-2222", MemberRole.MEMBER, MemberStatus.WITHDRAWN);
        MemberRequestDTO.LoginRequestDTO request = loginRequest("user1", "user1234");

        // when
        when(memberRepository.findByLoginId("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("user1234", "encoded")).thenReturn(true);

        // then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_WITHDRAWN);
    }

    @Test
    void 회원조회_성공() {
        // given
        Member member = member(7L, "user7", "pw7", "황민현", "010-1234-1234", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        MemberResponseDTO.MemberResultDTO result = memberService.getMemberInfo(7L);

        // then
        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getName()).isEqualTo("황민현");
    }

    @Test
    void 회원조회시_회원없으면_MEMBER_NOT_FOUND_예외() {

        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberInfo(99L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 출결요약조회시_출결통계와_보증금을_반환한다() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "user1", "pw", "홍길동", "010-1111-2222", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(
                11L, cohort, member, part(1L, cohort, PartType.SERVER), null, 85_000, 0
        );
        var clubSession = session(3L, cohort, "세션", LocalDateTime.now().minusDays(1), "강남", SessionStatus.COMPLETED);
        List<Attendance> attendances = List.of(
                attendance(1L, clubSession, member, cohortMember, null, AttendanceStatus.PRESENT, AttendanceSource.QR, Instant.now(), null, 0, null),
                attendance(2L, clubSession, member, cohortMember, null, AttendanceStatus.ABSENT, AttendanceSource.ADMIN, null, null, 10_000, "결석"),
                attendance(3L, clubSession, member, cohortMember, null, AttendanceStatus.LATE, AttendanceSource.QR, Instant.now(), 5, 2_500, null),
                attendance(4L, clubSession, member, cohortMember, null, AttendanceStatus.EXCUSED, AttendanceSource.ADMIN, null, null, 0, "병가")
        );

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(attendanceRepository.findAllByMemberIdOrderByCreatedAtAsc(1L)).thenReturn(attendances);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));

        MemberResponseDTO.AttendanceSummaryDTO summary = memberService.getAttendanceSummary(1L);

        // then
        assertThat(summary.getMemberId()).isEqualTo(1L);
        assertThat(summary.getPresent()).isEqualTo(1);
        assertThat(summary.getAbsent()).isEqualTo(1);
        assertThat(summary.getLate()).isEqualTo(1);
        assertThat(summary.getExcused()).isEqualTo(1);
        assertThat(summary.getTotalPenalty()).isEqualTo(12_500);
        assertThat(summary.getDeposit()).isEqualTo(85_000);
    }

    @Test
    void 출결요약조회시_기수회원없으면_보증금은_null() {
        // given
        Cohort cohort = cohort(2L, 11, "11기", true);
        Member member = member(2L, "user1", "pw", "홍길동", "010-1111-2222", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(attendanceRepository.findAllByMemberIdOrderByCreatedAtAsc(2L)).thenReturn(List.of());
        when(currentCohortProvider.getCurrentCohort()).thenReturn(cohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 2L)).thenReturn(Optional.empty());

        MemberResponseDTO.AttendanceSummaryDTO summary = memberService.getAttendanceSummary(2L);

        // then
        assertThat(summary.getDeposit()).isNull();
    }

    @Test
    void 출결요약조회시_회원없으면_MEMBER_NOT_FOUND_예외() {
        when(memberRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getAttendanceSummary(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
