package com.prography.backend.domain.attendance.service;

import com.prography.backend.domain.attendance.dto.AttendanceRequestDTO;
import com.prography.backend.domain.attendance.dto.AttendanceResponseDTO;
import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.domain.attendance.policy.AttendancePenaltyPolicy;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminAttendanceService {

    private static final int EXCUSE_LIMIT = 3;

    private final AttendanceRepository attendanceRepository;
    private final ClubSessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CurrentCohortProvider currentCohortProvider;
    private final DepositService depositService;
    private final AttendancePenaltyPolicy attendancePenaltyPolicy;

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
        Long sessionCohortId = session.getCohort().getId();
        CohortMember cohortMember = cohortMemberRepository.findByCohortIdAndMemberId(sessionCohortId, member.getId())
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
        Integer lateMinutes = attendancePenaltyPolicy.normalizeLateMinutes(status, request.getLateMinutes());
        int penaltyAmount = attendancePenaltyPolicy.calculatePenaltyAmount(status, lateMinutes);

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
                createPenaltyDescription(status, penaltyAmount)
        );

        return AttendanceResponseDTO.AttendanceResultDTO.from(attendance);
    }

    public AttendanceResponseDTO.AttendanceResultDTO updateAttendance(AttendanceRequestDTO.UpdateAttendanceRequestDTO request, Long attendanceId) {

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ApiException(ErrorCode.ATTENDANCE_NOT_FOUND));
        Long sessionCohortId = attendance.getSession().getCohort().getId();

        CohortMember cohortMember = cohortMemberRepository.findByCohortIdAndMemberId(
                        sessionCohortId,
                        attendance.getMember().getId()
                )
                .orElseThrow(() -> new ApiException(ErrorCode.COHORT_MEMBER_NOT_FOUND));

        // EXCUSED 상태 전환
        AttendanceStatus oldStatus = attendance.getStatus();
        AttendanceStatus newStatus = request.getStatus();
        if (oldStatus != AttendanceStatus.EXCUSED && newStatus == AttendanceStatus.EXCUSED) { // 다른 상태 → EXCUSED
            if (cohortMember.getExcuseCount() >= EXCUSE_LIMIT) {
                throw new ApiException(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
            }
            cohortMember.increaseExcuseCount();
        } else if (oldStatus == AttendanceStatus.EXCUSED && newStatus != AttendanceStatus.EXCUSED) { // EXCUSED → 다른 상태
            if (cohortMember.getExcuseCount() > 0) {
                cohortMember.decreaseExcuseCount();
            }
        }

        Integer newLateMinutes = attendancePenaltyPolicy.normalizeLateMinutes(newStatus, request.getLateMinutes());
        int oldPenalty = attendance.getPenaltyAmount();
        int newPenalty = attendancePenaltyPolicy.calculatePenaltyAmount(newStatus, newLateMinutes);
        int penaltyDiff = newPenalty - oldPenalty;

        if (penaltyDiff > 0) {
            depositService.applyPenalty(
                    cohortMember,
                    penaltyDiff,
                    attendance,
                    createPenaltyAdjustmentDescription(penaltyDiff)
            );
        } else if (penaltyDiff < 0) {
            depositService.applyRefund(
                    cohortMember,
                    -penaltyDiff,
                    attendance,
                    createRefundAdjustmentDescription(-penaltyDiff)
            );
        }

        attendance.updateByAdmin(newStatus, newLateMinutes, newPenalty, request.getReason());
        attendance = attendanceRepository.saveAndFlush(attendance);
        return AttendanceResponseDTO.AttendanceResultDTO.from(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO.SessionAttendanceSummaryDTO> getSessionAttendanceSummary(Long sessionId) {
        ClubSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();
        if (!session.getCohort().getId().equals(currentCohortId)) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }

        // 현재 기수(11기)의 전체 CohortMember 목록 조회
        List<CohortMember> cohortMembers = cohortMemberRepository.findAllByCohortIdOrderByIdAsc(currentCohortId);
        if (cohortMembers.isEmpty()) {
            return List.of();
        }

        List<Long> cohortMemberIds = cohortMembers.stream()
                .map(CohortMember::getId)
                .toList();
        List<Attendance> attendances = attendanceRepository.findAllByCohortMemberIdIn(cohortMemberIds);

        Map<Long, MemberAttendanceAccumulator> summaryMap = new HashMap<>();
        for (CohortMember cohortMember : cohortMembers) { // 각 회원의 전체 Attendance 레코드에서 상태별 count 집계
            summaryMap.put(cohortMember.getId(), new MemberAttendanceAccumulator());
        }

        for (Attendance attendance : attendances) {
            MemberAttendanceAccumulator accumulator = summaryMap.get(attendance.getCohortMember().getId());
            if (accumulator != null) {
                accumulator.add(attendance.getStatus(), attendance.getPenaltyAmount()); // totalPenalty = 전체 출결의 penaltyAmount 합계
            }
        }

        return cohortMembers.stream()
                .sorted(Comparator.comparing(cohortMember -> cohortMember.getMember().getId()))
                .map(cohortMember -> {
                    MemberAttendanceAccumulator accumulator = summaryMap.get(cohortMember.getId());
                    return AttendanceResponseDTO.SessionAttendanceSummaryDTO.from(
                            cohortMember.getMember().getId(),
                            cohortMember.getMember().getName(),
                            accumulator.present,
                            accumulator.absent,
                            accumulator.late,
                            accumulator.excused,
                            accumulator.totalPenalty, // deposit = CohortMember.deposit
                            cohortMember.getDepositBalance()
                    );
                })
                .toList();
    }

    public AttendanceResponseDTO.AttendanceDetailDTO getAttendanceDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();

        CohortMember cohortMember = cohortMemberRepository.findByCohortIdAndMemberId(
                        currentCohortId,
                        memberId
                )
                .orElse(null);

        return AttendanceResponseDTO.AttendanceDetailDTO.from(
                member,
                cohortMember,
                attendanceRepository.findAllByMemberIdOrderByCreatedAtAsc(memberId)
        );
    }

    public AttendanceResponseDTO.SessionAttendanceDetailDTO getSessionAttendanceDetail(Long sessionId) {
        ClubSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();
        if (!session.getCohort().getId().equals(currentCohortId)) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }

        List<Attendance> attendances = attendanceRepository.findAllBySessionIdOrderByCheckedAtAsc(sessionId);
        return AttendanceResponseDTO.SessionAttendanceDetailDTO.from(session, attendances);
    }

    private String createPenaltyDescription(AttendanceStatus status, int penaltyAmount) {
        return "출결 등록 - " + status + " 패널티 " + penaltyAmount + "원";
    }

    private String createPenaltyAdjustmentDescription(int additionalPenaltyAmount) {
        return "출결 수정 - 추가 패널티 " + additionalPenaltyAmount + "원";
    }

    private String createRefundAdjustmentDescription(int refundAmount) {
        return "출결 수정 - 환급 " + refundAmount + "원";
    }

    private static final class MemberAttendanceAccumulator {
        private int present;
        private int absent;
        private int late;
        private int excused;
        private int totalPenalty;

        private void add(AttendanceStatus status, int penaltyAmount) {
            if (status == AttendanceStatus.PRESENT) {
                present++;
            } else if (status == AttendanceStatus.ABSENT) {
                absent++;
            } else if (status == AttendanceStatus.LATE) {
                late++;
            } else if (status == AttendanceStatus.EXCUSED) {
                excused++;
            }
            totalPenalty += penaltyAmount;
        }
    }
}
