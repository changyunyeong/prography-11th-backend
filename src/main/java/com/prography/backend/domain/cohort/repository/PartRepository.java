package com.prography.backend.domain.cohort.repository;

import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.global.common.enums.PartType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByCohortIdAndType(Long cohortId, PartType type);
    List<Part> findAllByCohortIdOrderByIdAsc(Long cohortId);
}
