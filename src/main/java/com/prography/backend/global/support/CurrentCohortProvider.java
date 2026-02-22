package com.prography.backend.global.support;

import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CurrentCohortProvider {

    private final CohortRepository cohortRepository;
    private final String currentCohortName;

    public CurrentCohortProvider(CohortRepository cohortRepository, @Value("${app.current-cohort-name}") String currentCohortName) {
        this.cohortRepository = cohortRepository;
        this.currentCohortName = currentCohortName;
    }

    public Cohort getCurrentCohort() {
        return cohortRepository.findByName(currentCohortName)
            .or(() -> cohortRepository.findByCurrentOperatingTrue())
            .orElseThrow(() -> new ApiException(ErrorCode.COHORT_NOT_FOUND));
    }
}
