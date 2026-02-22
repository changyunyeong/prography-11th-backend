package com.prography.backend.domain.cohort.service;

import com.prography.backend.domain.cohort.dto.CohortResponseDTO;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;

    @Transactional(readOnly = true)
    public List<CohortResponseDTO.CohortListDTO> getCohortList() {
        List<Cohort> cohorts = cohortRepository.findAll(Sort.by(Sort.Direction.ASC, "generation"));
        return cohorts.stream()
                .map(CohortResponseDTO.CohortListDTO::from)
                .toList();
    }
}
