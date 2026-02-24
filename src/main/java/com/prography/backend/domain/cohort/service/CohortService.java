package com.prography.backend.domain.cohort.service;

import com.prography.backend.domain.cohort.dto.CohortResponseDTO;
import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import com.prography.backend.domain.cohort.repository.PartRepository;
import com.prography.backend.domain.cohort.repository.TeamRepository;
import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.domain.deposit.repository.DepositHistoryRepository;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final DepositHistoryRepository depositHistoryRepository;

    public List<CohortResponseDTO.CohortListDTO> getCohortList() {
        List<Cohort> cohorts = cohortRepository.findAll(Sort.by(Sort.Direction.ASC, "generation"));
        return cohorts.stream()
                .map(CohortResponseDTO.CohortListDTO::from)
                .toList();
    }

    public CohortResponseDTO.CohortDetailDTO getCohortDetail(Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ApiException(ErrorCode.COHORT_NOT_FOUND));

        List<Part> parts = partRepository.findAllByCohortIdOrderByIdAsc(cohortId);
        List<Team> teams = teamRepository.findAllByCohortIdOrderByIdAsc(cohortId);

        return CohortResponseDTO.CohortDetailDTO.from(cohort, parts, teams);
    }

    public List<CohortResponseDTO.DepositHistoryDTO> getDepositHistory(Long cohortMemberId) {
        CohortMember cohortMember = cohortMemberRepository.findById(cohortMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.COHORT_MEMBER_NOT_FOUND));
        List<DepositHistory> depositHistory = depositHistoryRepository.findAllByCohortMemberIdOrderByCreatedAtAsc(cohortMemberId);
        return depositHistory.stream()
                .map(CohortResponseDTO.DepositHistoryDTO::from)
                .toList();
    }
}
