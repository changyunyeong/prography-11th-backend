package com.prography.backend.domain.cohort.entity;

import com.prography.backend.global.common.base.BaseEntity;
import com.prography.backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cohort_members")
public class CohortMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "deposit_balance", nullable = false)
    private int depositBalance;

    @Column(name = "excuse_count", nullable = false)
    private int excuseCount;

    public void increaseDeposit(int amount) {
        this.depositBalance += amount;
    }

    public void decreaseDeposit(int amount) {
        this.depositBalance -= amount;
    }

    public void updatePart(Part part) {
        this.part = part;
    }

    public void updateTeam(Team team) {
        this.team = team;
    }

}
