package com.prography.backend.domain.session.service;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.domain.session.dto.SessionResponseDTO;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.domain.session.repository.ClubSessionRepository;
import com.prography.backend.global.support.CurrentCohortProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SessionService {

    private final ClubSessionRepository clubSessionRepository;
    private final CurrentCohortProvider currentCohortProvider;

    public List<SessionResponseDTO.SessionInfoDTO> getSessionList() {
        Cohort cohort = currentCohortProvider.getCurrentCohort(); // current-cohort.generation=11 설정에서 현재 기수 ID 결정
        List<ClubSession> sessions = clubSessionRepository.findAllByCohortIdOrderByStartsAtAsc(cohort.getId());

        return sessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .map(SessionResponseDTO.SessionInfoDTO::from)
                .toList();
    }
}
