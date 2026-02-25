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
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.qrcode.repository.QrCodeRepository;
import com.prography.backend.domain.session.repository.ClubSessionRepository;
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.support.CurrentCohortProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService {

    private static final int ABSENT_PENALTY = 10_000;
    private static final int LATE_PENALTY_PER_MINUTE = 500;
    private static final int MAX_LATE_PENALTY = 10_000;

    private final QrCodeRepository qrCodeRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DepositService depositService;
    private final CurrentCohortProvider currentCohortProvider;

    public AttendanceResponseDTO.AttendanceResultDTO checkAttendance(AttendanceRequestDTO.CheckAttendanceRequestDTO request) {

        // QR hashValue로 QrCode 조회 → 없으면 QR_INVALID
        QrCode qrCode = qrCodeRepository.findByHashValue(request.getHashValue())
                .orElseThrow(() -> new ApiException(ErrorCode.QR_INVALID));

        // QR 만료 검증 (expiresAt < 현재 시각) → 만료면 QR_EXPIRED
        if (qrCode.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.QR_EXPIRED);
        }

        // QrCode의 sessionId로 Session 조회 → 상태가 IN_PROGRESS 아니면 SESSION_NOT_IN_PROGRESS
        if (qrCode.getSession().getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }

        // memberId로 회원 조회 → 없으면 MEMBER_NOT_FOUND
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 회원 상태 WITHDRAWN 확인 → MEMBER_WITHDRAWN
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ApiException(ErrorCode.MEMBER_WITHDRAWN);
        }

        // (sessionId, memberId) 중복 출결 확인 → 이미 존재하면 ATTENDANCE_ALREADY_CHECKED
        boolean alreadyExists = attendanceRepository.existsBySessionIdAndMemberId(qrCode.getSession().getId(), member.getId());
        if (alreadyExists) {
            throw new ApiException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
        }

        // 현재 기수의 CohortMember 존재 확인 → 없으면 COHORT_MEMBER_NOT_FOUND
        Long currentCohortId = currentCohortProvider.getCurrentCohort().getId();
        CohortMember cohortMember = cohortMemberRepository.findByCohortIdAndMemberId(currentCohortId, member.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.COHORT_MEMBER_NOT_FOUND));

        // 지각 시간 계산:
        // session.date + session.time (Asia/Seoul 타임존) vs 현재 시각
        // 현재 > 일정시각 → LATE (지각분 = 차이(분))
        // 현재 <= 일정시각 → PRESENT
        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");
        LocalDateTime nowInSeoul = LocalDateTime.now(seoulZoneId);
        LocalDateTime sessionStartsAt = qrCode.getSession().getStartsAt();

        AttendanceStatus status;
        Integer lateMinutes = null;
        if (nowInSeoul.isAfter(sessionStartsAt)) {
            status = AttendanceStatus.LATE;
            lateMinutes = Math.toIntExact(ChronoUnit.MINUTES.between(sessionStartsAt, nowInSeoul));
        } else {
            status = AttendanceStatus.PRESENT;
        }

        // 패널티 계산:
        // PRESENT → 0원
        // LATE → min(지각분 × 500, 10,000)원
        int penaltyAmount = calculatePenaltyAmount(status, lateMinutes);

        // Attendance 저장 (qrCodeId, checkedInAt 포함)
        Attendance attendance = Attendance.builder()
                .session(qrCode.getSession())
                .member(member)
                .cohortMember(cohortMember)
                .qrCode(qrCode)
                .status(status)
                .source(AttendanceSource.QR)
                .checkedAt(Instant.now())
                .latenessMinutes(lateMinutes)
                .penaltyAmount(penaltyAmount)
                .reason(null)
                .build();
        attendance = attendanceRepository.saveAndFlush(attendance);

        // 패널티 > 0이면:
        // CohortMember.deposit에서 차감
        // DepositHistory(type=PENALTY) 기록
        // 잔액 부족 시 DEPOSIT_INSUFFICIENT
        depositService.applyPenalty(
                cohortMember,
                penaltyAmount,
                attendance,
                createPenaltyDescription(status, penaltyAmount)
        );

        return AttendanceResponseDTO.AttendanceResultDTO.from(attendance);
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

    private String createPenaltyDescription(AttendanceStatus status, int penaltyAmount) {
        return "출결 등록 - " + status + " 패널티 " + penaltyAmount + "원";
    }
}
