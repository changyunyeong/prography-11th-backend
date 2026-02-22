package com.prography.backend.domain.cohort.entity;

import com.prography.backend.global.common.base.BaseEntity;
import com.prography.backend.global.common.enums.PartType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "parts")
public class Part extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartType type;

}
