package com.prography.backend.domain.member.service;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.deposit.service.DepositService;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.attendance.repository.AttendanceRepository;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.domain.cohort.repository.PartRepository;
import com.prography.backend.domain.cohort.repository.TeamRepository;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private static final int INITIAL_DEPOSIT = 100_000;

    private final MemberRepository memberRepository;
    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepositService depositService;
    private final CurrentCohortProvider currentCohortProvider;

    public MemberResponseDTO.MemberCreateResponseDTO createMember(MemberRequestDTO.CreateMemberRequestDTO request) {

        // loginId 중복 검사
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        // cohortId, partId, teamId 존재 검증
        Cohort cohort = currentCohortProvider.getCurrentCohort();
        if (request.getCohortId() != null && !request.getCohortId().equals(cohort.getId())) {
            throw new ApiException(ErrorCode.COHORT_NOT_FOUND);
        }

        Part part = null;
        Long partId = request.getPartId();
        if (partId != null) {
            part = partRepository.findById(partId)
                    .filter(value -> value.getCohort().getId().equals(cohort.getId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.PART_NOT_FOUND));
        }

        Team team = null;
        Long teamId = request.getTeamId();
        if (teamId != null) {
            team = teamRepository.findById(teamId)
                    .filter(value -> value.getCohort().getId().equals(cohort.getId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.TEAM_NOT_FOUND));
        }

        // 비밀번호 BCrypt 해싱 (cost factor 12) && Member 생성 (status=ACTIVE, role=MEMBER)
        Member member = Member.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.MEMBER)
                .build();
        member = memberRepository.save(member);

        // CohortMember 생성 (deposit=100,000원)
        CohortMember cohortMember = CohortMember.builder()
                .member(member)
                .cohort(cohort)
                .part(part)
                .team(team)
                .depositBalance(0)
                .build();
        cohortMemberRepository.save(cohortMember);

        // DepositHistory 생성 (type=INITIAL, amount=100,000원)
        depositService.initializeDeposit(cohortMember, INITIAL_DEPOSIT, "초기 보증금 설정");

        return MemberResponseDTO.MemberCreateResponseDTO.from(member, cohortMember);
    }
    
}
