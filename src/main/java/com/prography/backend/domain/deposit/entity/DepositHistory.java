package com.prography.backend.domain.deposit.entity;

import com.prography.backend.domain.attendance.entity.Attendance;
import com.prography.backend.global.common.base.BaseEntity;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.global.common.enums.DepositType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "deposit_histories")
public class DepositHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_member_id", nullable = false)
    private CohortMember cohortMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepositType type;

    @Column(nullable = false)
    private int amount;

    @Column(name = "balance_before", nullable = false)
    private int balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @Column(length = 300)
    private String description;

}
