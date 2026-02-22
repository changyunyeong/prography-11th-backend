package com.prography.backend.domain.cohort.repository;

import com.prography.backend.domain.cohort.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByCohortIdAndName(Long cohortId, String name);
    List<Team> findAllByCohortIdOrderByIdAsc(Long cohortId);
}
