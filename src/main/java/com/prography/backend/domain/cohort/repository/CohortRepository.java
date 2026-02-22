package com.prography.backend.domain.cohort.repository;

import com.prography.backend.domain.cohort.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByName(String name);
    Optional<Cohort> findByCurrentOperatingTrue();
}
