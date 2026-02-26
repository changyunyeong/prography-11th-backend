package com.prography.backend.domain.session.service;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.session.dto.SessionResponseDTO;
import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.domain.session.repository.ClubSessionRepository;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.support.CurrentCohortProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private ClubSessionRepository clubSessionRepository;

    @Mock
    private CurrentCohortProvider currentCohortProvider;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void 세션목록조회시_취소된일정은_제외된다() {
        // given
        Cohort currentCohort = cohort(2L, 11, "11기", true);
        ClubSession scheduled = session(1L, currentCohort, "정기모임", LocalDateTime.now().plusDays(1), "강남", SessionStatus.SCHEDULED);
        ClubSession cancelled = session(2L, currentCohort, "취소됨", LocalDateTime.now().plusDays(2), "강남", SessionStatus.CANCELLED);

        // when
        when(currentCohortProvider.getCurrentCohort()).thenReturn(currentCohort);
        when(clubSessionRepository.findAllByCohortIdOrderByStartsAtAsc(2L))
                .thenReturn(List.of(scheduled, cancelled));

        List<SessionResponseDTO.SessionInfoDTO> result = sessionService.getSessionList();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getStatus()).isEqualTo(SessionStatus.SCHEDULED);
    }
}
