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
import com.prography.backend.global.common.enums.DepositType;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.PartType;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.prography.backend.support.TestFixtures.cohort;
import static com.prography.backend.support.TestFixtures.cohortMember;
import static com.prography.backend.support.TestFixtures.depositHistory;
import static com.prography.backend.support.TestFixtures.member;
import static com.prography.backend.support.TestFixtures.part;
import static com.prography.backend.support.TestFixtures.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CohortServiceTest {

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private DepositHistoryRepository depositHistoryRepository;

    @InjectMocks
    private CohortService cohortService;

    @Test
    void 기수목록조회시_성공() {
        // given
        Cohort cohort10 = cohort(1L, 10, "10기", false);
        Cohort cohort11 = cohort(2L, 11, "11기", true);

        // when
        when(cohortRepository.findAll(Sort.by(Sort.Direction.ASC, "generation")))
                .thenReturn(List.of(cohort10, cohort11));

        List<CohortResponseDTO.CohortListDTO> result = cohortService.getCohortList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getGeneration()).isEqualTo(10);
        assertThat(result.get(1).getGeneration()).isEqualTo(11);
    }

    @Test
    void 기수상세조회_성공() {
        // given
        Cohort cohort11 = cohort(2L, 11, "11기", true);
        Part server = part(6L, cohort11, PartType.SERVER);
        Part web = part(7L, cohort11, PartType.WEB);
        Part ios = part(8L, cohort11, PartType.IOS);
        Part android = part(9L, cohort11, PartType.ANDROID);
        Part design = part(10L, cohort11, PartType.DESIGN);
        Team teamA = team(1L, cohort11, "Team A");
        Team teamB = team(2L, cohort11, "Team B");
        Team teamC = team(3L, cohort11, "Team C");

        // when
        when(cohortRepository.findById(2L)).thenReturn(Optional.of(cohort11));
        when(partRepository.findAllByCohortIdOrderByIdAsc(2L))
                .thenReturn(List.of(server, web, ios, android, design));
        when(teamRepository.findAllByCohortIdOrderByIdAsc(2L))
                .thenReturn(List.of(teamA, teamB, teamC));

        CohortResponseDTO.CohortDetailDTO result = cohortService.getCohortDetail(2L);

        // then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getGeneration()).isEqualTo(11);
        assertThat(result.getName()).isEqualTo("11기");

        assertThat(result.getParts())
                .extracting(CohortResponseDTO.CohortPartDTO::getName)
                .containsExactly("SERVER", "WEB", "iOS", "ANDROID", "DESIGN");
        assertThat(result.getTeams())
                .extracting(CohortResponseDTO.CohortTeamDTO::getName)
                .containsExactly("Team A", "Team B", "Team C");
    }

    @Test
    void 기수상세조회시_기수가없으면_COHORT_NOT_FOUND_예외() {
        when(cohortRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cohortService.getCohortDetail(404L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COHORT_NOT_FOUND);
    }

    @Test
    void 보증금이력조회시_금액부호가_정상반영된다() {
        // given
        Cohort cohort11 = cohort(2L, 11, "11기", true);
        var member = member(10L, "user1", "pw", "홍길동", "010", MemberRole.MEMBER, MemberStatus.ACTIVE);
        CohortMember cohortMember = cohortMember(1L, cohort11, member, null, null, 90_000, 0);
        DepositHistory initial = depositHistory(100L, cohortMember, DepositType.INITIAL, 100_000, 0, 100_000, null, "초기");
        DepositHistory penalty = depositHistory(101L, cohortMember, DepositType.PENALTY, 10_000, 100_000, 90_000, null, "패널티");
        DepositHistory refund = depositHistory(102L, cohortMember, DepositType.REFUND, 5_000, 90_000, 95_000, null, "환급");

        // when
        when(cohortMemberRepository.findById(1L)).thenReturn(Optional.of(cohortMember));
        when(depositHistoryRepository.findAllByCohortMemberIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(initial, penalty, refund));

        List<CohortResponseDTO.DepositHistoryDTO> result = cohortService.getDepositHistory(1L);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAmount()).isEqualTo(100_000);
        assertThat(result.get(1).getAmount()).isEqualTo(-10_000);
        assertThat(result.get(2).getAmount()).isEqualTo(5_000);
    }

    @Test
    void 보증금이력조회시_기수회원이없으면_COHORT_MEMBER_NOT_FOUND_예외() {

        when(cohortMemberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cohortService.getDepositHistory(999L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COHORT_MEMBER_NOT_FOUND);
    }
}
