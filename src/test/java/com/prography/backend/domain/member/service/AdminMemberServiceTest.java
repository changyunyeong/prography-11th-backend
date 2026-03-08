package com.prography.backend.domain.member.service;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import com.prography.backend.domain.cohort.repository.PartRepository;
import com.prography.backend.domain.cohort.repository.TeamRepository;
import com.prography.backend.domain.deposit.service.DepositService;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.PartType;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.prography.backend.global.support.TestFixtures.cohort;
import static com.prography.backend.global.support.TestFixtures.cohortMember;
import static com.prography.backend.global.support.TestFixtures.createMemberRequest;
import static com.prography.backend.global.support.TestFixtures.member;
import static com.prography.backend.global.support.TestFixtures.part;
import static com.prography.backend.global.support.TestFixtures.team;
import static com.prography.backend.global.support.TestFixtures.updateMemberRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DepositService depositService;

    @Mock
    private CurrentCohortProvider currentCohortProvider;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    void 회원등록_성공() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        Part part = part(6L, currentCohort, PartType.SERVER);
        Team team = team(1L, currentCohort, "Team A");
        MemberRequestDTO.CreateMemberRequestDTO request =
                createMemberRequest("user1", "rawPw", "홍길동", "010-1111-2222", 2L, 6L, 1L);

        // when
        when(memberRepository.existsByLoginId("user1")).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);
        when(partRepository.findById(6L)).thenReturn(Optional.of(part));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(passwordEncoder.encode("rawPw")).thenReturn("encodedPw");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member saved = invocation.getArgument(0, Member.class);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        when(cohortMemberRepository.save(any(CohortMember.class))).thenAnswer(invocation -> {
            CohortMember saved = invocation.getArgument(0, CohortMember.class);
            ReflectionTestUtils.setField(saved, "id", 200L);
            return saved;
        });

        MemberResponseDTO.MemberAdminResultDTO result = adminMemberService.createMember(request);

        // then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class); // mock 객체의 메서드가 호출될 때 전달된 인자를 캡처(포획)하여 나중에 검증할 수 있도록 함
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getPassword()).isEqualTo("encodedPw");
        assertThat(savedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.MEMBER);
        /**
         서비스 → save(member) → Repository(mock)
         ↑
         ArgumentCaptor가 여기서 member를 낚아챔
         *
         */

        verify(depositService).initializeDeposit(any(CohortMember.class), eq(100_000), eq("초기 보증금"));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getLoginId()).isEqualTo("user1");
        assertThat(result.getGeneration()).isEqualTo(11);
        assertThat(result.getPartName()).isEqualTo("SERVER");
        assertThat(result.getTeamName()).isEqualTo("Team A");
    }

    @Test
    void 회원등록시_중복로그인아이디면_DUPLICATE_LOGIN_ID_예외() {
        // given
        MemberRequestDTO.CreateMemberRequestDTO request =
                createMemberRequest("dup", "pw", "홍길동", "010", 2L, null, null);

        // when
        when(memberRepository.existsByLoginId("dup")).thenReturn(true);

        // then
        assertThatThrownBy(() -> adminMemberService.createMember(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
    }

    @Test
    void 회원등록시_요청기수가_현재기수가아니면_COHORT_NOT_FOUND_예외() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        MemberRequestDTO.CreateMemberRequestDTO request =
                createMemberRequest("user1", "pw", "홍길동", "010", 10L, null, null); // 10기

        when(memberRepository.existsByLoginId("user1")).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);

        // when
        assertThatThrownBy(() -> adminMemberService.createMember(request))
        // then
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COHORT_NOT_FOUND);
    }

    @Test
    void 회원등록시_파트없으면_PART_NOT_FOUND_예외() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        MemberRequestDTO.CreateMemberRequestDTO request =
                createMemberRequest("user1", "pw", "홍길동", "010", 2L, 999L, null);

        // when
        when(memberRepository.existsByLoginId("user1")).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);
        when(partRepository.findById(999L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminMemberService.createMember(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PART_NOT_FOUND);
    }

    @Test
    void 회원등록시_팀없으면_TEAM_NOT_FOUND_예외() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        MemberRequestDTO.CreateMemberRequestDTO request =
                createMemberRequest("user1", "pw", "홍길동", "010", 2L, null, 999L);

        // when
        when(memberRepository.existsByLoginId("user1")).thenReturn(false);
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminMemberService.createMember(request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    void 회원대시보드시_검색타입값쌍이불완전하면_INVALID_INPUT_예외() {

        assertThatThrownBy(() ->
                adminMemberService.getMemberDashboard(0, 10, "name", null, null, null, null, null)
        )
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 회원대시보드시_필터적용후_총개수를_재계산한다() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        Member member1 = member(1L, "user1", "pw", "홍길동", "0101", MemberRole.MEMBER, MemberStatus.ACTIVE);
        Member member2 = member(2L, "user2", "pw", "김철수", "0102", MemberRole.MEMBER, MemberStatus.ACTIVE);

        CohortMember cohortMember1 = cohortMember(
                10L, currentCohort, member1, part(1L, currentCohort, PartType.SERVER), team(1L, currentCohort, "Team A"), 90_000, 0
        );
        CohortMember cohortMember2 = cohortMember(
                20L, currentCohort, member2, part(2L, currentCohort, PartType.WEB), team(2L, currentCohort, "Team B"), 85_000, 0
        );

        // when
        when(memberRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(member2, member1));
        when(cohortMemberRepository.findAllByMemberIdIn(List.of(2L, 1L)))
                .thenReturn(List.of(cohortMember1, cohortMember2));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);

        MemberResponseDTO.MemberDashboardPreViewListDTO result = adminMemberService.getMemberDashboard(
                0, 10, null, null, 11, "server", null, MemberStatus.ACTIVE
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void 회원상세조회시_회원없으면_MEMBER_NOT_FOUND_예외() {

        when(memberRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMemberService.getMemberDetail(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 회원수정시_기수필드없으면_이름과전화번호만_수정된다() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        Member member = member(1L, "user1", "pw", "홍길동", "010-1111", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(10L, currentCohort, member, null, null, 100_000, 0);
        MemberRequestDTO.UpdateMemberRequestDTO request = updateMemberRequest("새이름", "010-9999", null, null, null);

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.of(cohortMember));

        MemberResponseDTO.MemberAdminResultDTO result = adminMemberService.updateMember(1L, request);

        // then
        assertThat(result.getName()).isEqualTo("새이름");
        assertThat(result.getPhone()).isEqualTo("010-9999");
    }

    @Test
    void 회원수정시_대상기수회원이없으면_신규생성한다() {
        // given
        Cohort cohort11 = cohort(2L, 11, "11기", true);
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        Part part = part(6L, cohort11, PartType.SERVER);
        Team team = team(1L, cohort11, "Team A");
        MemberRequestDTO.UpdateMemberRequestDTO request = updateMemberRequest(null, null, 2L, 6L, 1L);

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort11));
        when(partRepository.findById(6L)).thenReturn(Optional.of(part));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(cohortMemberRepository.findByCohortIdAndMemberId(2L, 1L)).thenReturn(Optional.empty());
        when(cohortMemberRepository.save(any(CohortMember.class))).thenAnswer(invocation -> {
            CohortMember saved = invocation.getArgument(0, CohortMember.class);
            ReflectionTestUtils.setField(saved, "id", 999L);
            return saved;
        });

        MemberResponseDTO.MemberAdminResultDTO result = adminMemberService.updateMember(1L, request);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getGeneration()).isEqualTo(11);
        assertThat(result.getPartName()).isEqualTo("SERVER");
        assertThat(result.getTeamName()).isEqualTo("Team A");
    }

    @Test
    void 회원수정시_대상기수에없는파트면_PART_NOT_FOUND_예외() {
        // given
        Cohort targetCohort = cohort(2L, 11, "11기", true);
        Cohort otherCohort = cohort(1L, 10, "10기", false);
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        Part wrongPart = part(6L, otherCohort, PartType.SERVER);
        MemberRequestDTO.UpdateMemberRequestDTO request = updateMemberRequest(null, null, 2L, 6L, null);

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(targetCohort));
        when(partRepository.findById(6L)).thenReturn(Optional.of(wrongPart));

        // then
        assertThatThrownBy(() -> adminMemberService.updateMember(1L, request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PART_NOT_FOUND);
    }

    @Test
    void 회원탈퇴시_소프트삭제된다() {
        // given
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.saveAndFlush(member)).thenReturn(member);

        MemberResponseDTO.MemberDeleteDTO result = adminMemberService.deleteMember(1L);

        // then
        assertThat(result.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    void 회원탈퇴시_이미탈퇴상태면_MEMBER_ALREADY_WITHDRAWN_예외() {
        // given
        Member member = member(1L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.WITHDRAWN);

        // when
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        // then
        assertThatThrownBy(() -> adminMemberService.deleteMember(1L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
    }
}
