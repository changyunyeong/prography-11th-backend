package com.prography.backend.domain.member.service;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.attendance.repository.AttendanceRepository;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CurrentCohortProvider currentCohortProvider;
    private final PasswordEncoder passwordEncoder;

    public MemberResponseDTO.MemberResultDTO login(MemberRequestDTO.LoginRequestDTO request) {

        // loginId로 회원 조회 → 없으면 LOGIN_FAILED
        // BCrypt 비밀번호 검증 → 불일치 시 LOGIN_FAILED
        Member member =  memberRepository.findByLoginId(request.getLoginId())
                .filter(m -> passwordEncoder.matches(request.getPassword(), m.getPassword()))
                .orElseThrow(
                        () -> new ApiException(ErrorCode.LOGIN_FAILED)
                );

        // 회원 상태가 WITHDRAWN이면 MEMBER_WITHDRAWN
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ApiException(ErrorCode.MEMBER_WITHDRAWN);
        }

        return MemberResponseDTO.MemberResultDTO.from(member);
    }

    public MemberResponseDTO.MemberResultDTO getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        return MemberResponseDTO.MemberResultDTO.from(member);
    }

    @Transactional(readOnly = true)
    public MemberResponseDTO.AttendanceSummaryDTO getAttendanceSummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        List<Attendance> attendances = attendanceRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId);

        int present = 0;
        int absent = 0;
        int late = 0;
        int excused = 0;
        int totalPenalty = 0;

        // 회원의 전체 Attendance 레코드에서 상태별 count 집계
        for (Attendance attendance : attendances) {
            if (attendance.getStatus() == AttendanceStatus.PRESENT) {
                present++;
            } else if (attendance.getStatus() == AttendanceStatus.ABSENT) {
                absent++;
            } else if (attendance.getStatus() == AttendanceStatus.LATE) {
                late++;
            } else if (attendance.getStatus() == AttendanceStatus.EXCUSED) {
                excused++;
            }
            // totalPenalty는 모든 출결의 penaltyAmount 합계
            totalPenalty += attendance.getPenaltyAmount();
        }

        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();
        // deposit은 현재 기수(11기) CohortMember의 잔액 (CohortMember가 없으면 null)
        Integer deposit = cohortMemberRepository.findByCohortIdAndMemberId(currentCohortId, memberId)
                .map(CohortMember::getDepositBalance)
                .orElse(null);

        return MemberResponseDTO.AttendanceSummaryDTO.of(
                member.getId(),
                present,
                absent,
                late,
                excused,
                totalPenalty,
                deposit
        );
    }
}
