package com.prography.backend.domain.attendance.entity;

import com.prography.backend.global.common.base.BaseEntity;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.qrcode.entity.QrCode;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.global.common.enums.AttendanceSource;
import com.prography.backend.global.common.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "attendances")
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClubSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_member_id", nullable = false)
    private CohortMember cohortMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id")
    private QrCode qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSource source;

    @Column(name = "checked_at")
    private Instant checkedAt;

    @Column(name = "lateness_minutes")
    private Integer latenessMinutes;

    @Column(name = "penalty_amount", nullable = false)
    private int penaltyAmount;

    @Column(length = 500)
    private String reason;

    public void updateByAdmin(AttendanceStatus status, Integer latenessMinutes, int penaltyAmount, String reason) {
        this.status = status;
        this.latenessMinutes = latenessMinutes;
        this.penaltyAmount = penaltyAmount;
        if (reason != null) {
            this.reason = reason;
        }
    }

}
