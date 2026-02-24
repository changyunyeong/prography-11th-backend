package com.prography.backend.domain.attendance.service;

import com.prography.backend.domain.attendance.dto.AttendanceRequestDTO;
import com.prography.backend.domain.attendance.dto.AttendanceResponseDTO;
import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.attendance.repository.AttendanceRepository;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.deposit.service.DepositService;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.domain.session.repository.ClubSessionRepository;
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService {

    private static final int ABSENT_PENALTY = 10_000;
    private static final int LATE_PENALTY_PER_MINUTE = 500;
    private static final int MAX_LATE_PENALTY = 10_000;
    private static final int EXCUSE_LIMIT = 3;

    private final AttendanceRepository attendanceRepository;
    private final ClubSessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CurrentCohortProvider currentCohortProvider;
    private final DepositService depositService;

    public AttendanceResponseDTO.AttendanceResultDTO registerAttendance(AttendanceRequestDTO.RegisterAttendanceRequestDTO request) {
        // 일정/회원 존재 검증
        ClubSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // (sessionId, memberId) 중복 출결 확인
        boolean alreadyExists = attendanceRepository.existsBySessionIdAndMemberId(session.getId(), member.getId());
        if (alreadyExists) {
            throw new ApiException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
        }

        // CohortMember 존재 확인
        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();
        CohortMember cohortMember = cohortMemberRepository.findByCohortIdAndMemberId(currentCohortId, member.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.COHORT_MEMBER_NOT_FOUND));

        // EXCUSED 등록 시: excuseCount < 3 검증 → 통과 시 excuseCount++
        AttendanceStatus status = request.getStatus();
        if (status == AttendanceStatus.EXCUSED) {
            if (cohortMember.getExcuseCount() >= EXCUSE_LIMIT) {
                throw new ApiException(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
            }
            cohortMember.increaseExcuseCount();
        }

        // 패널티 계산:
        // PRESENT → 0원
        // ABSENT → 10,000원
        // LATE → min(lateMinutes × 500, 10,000)원
        // EXCUSED → 0원
        Integer lateMinutes = resolveLateMinutes(status, request.getLateMinutes());
        int penaltyAmount = calculatePenaltyAmount(status, lateMinutes);

        Attendance attendance = Attendance.builder()
                .session(session)
                .member(member)
                .cohortMember(cohortMember)
                .status(status)
                .source(AttendanceSource.ADMIN)
                .checkedAt(null)
                .latenessMinutes(lateMinutes)
                .penaltyAmount(penaltyAmount)
                .reason(request.getReason())
                .build();
        attendance = attendanceRepository.saveAndFlush(attendance);

        // 패널티 > 0이면 보증금 차감 + DepositHistory(PENALTY) 기록
        depositService.applyPenalty(
                cohortMember,
                penaltyAmount,
                attendance,
                createPenaltyDescription(status, request.getReason())
        );

        return AttendanceResponseDTO.AttendanceResultDTO.from(attendance);
    }

    private Integer resolveLateMinutes(AttendanceStatus status, Integer lateMinutes) {
        if (status != AttendanceStatus.LATE) {
            return null;
        }

        if (lateMinutes == null || lateMinutes < 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        return lateMinutes;
    }

    private int calculatePenaltyAmount(AttendanceStatus status, Integer lateMinutes) {
        if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.EXCUSED) {
            return 0;
        }
        if (status == AttendanceStatus.ABSENT) {
            return ABSENT_PENALTY;
        }

        long latePenalty = (long) lateMinutes * LATE_PENALTY_PER_MINUTE;
        return (int) Math.min(latePenalty, MAX_LATE_PENALTY);
    }

    private String createPenaltyDescription(AttendanceStatus status, String reason) {
        if (reason == null || reason.isBlank()) {
            return "출결 패널티 차감(" + status + ")";
        }
        return "출결 패널티 차감(" + status + "): " + reason.trim();
    }
}
