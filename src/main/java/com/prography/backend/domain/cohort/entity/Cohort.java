package com.prography.backend.domain.cohort.entity;

import com.prography.backend.global.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "cohort", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Part> partList = new ArrayList<>();

    @OneToMany(mappedBy = "cohort", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teamList = new ArrayList<>();

    @Column(name = "is_current", nullable = false)
    private boolean currentOperating;

}
