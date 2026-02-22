package com.prography.backend.domain.cohort.entity;

import com.prography.backend.global.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cohorts")
public class Cohort extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer generation;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "is_current", nullable = false)
    private boolean currentOperating;

}
