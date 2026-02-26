package com.prography.backend.global.bootstrap;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import com.prography.backend.domain.cohort.repository.PartRepository;
import com.prography.backend.domain.cohort.repository.TeamRepository;
import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.domain.deposit.repository.DepositHistoryRepository;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.global.common.enums.DepositType;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.enums.PartType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seed-data-test;MODE=MYSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.current-cohort-name=11기"
})
@Transactional
class SeedDataInitializerIntegrationTest {

    @Autowired
    private SeedDataInitializer seedDataInitializer;

    @Autowired
    private CohortRepository cohortRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CohortMemberRepository cohortMemberRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @Test
    void 서버시작시_시드데이터가_요구사항대로_생성된다() {
        // given
        Cohort cohort10 = cohortRepository.findByName("10기").orElseThrow();
        Cohort cohort11 = cohortRepository.findByName("11기").orElseThrow();
        Member admin = memberRepository.findByLoginId("admin").orElseThrow();
        CohortMember adminCohortMember = cohortMemberRepository
                .findByCohortIdAndMemberId(cohort11.getId(), admin.getId())
                .orElseThrow();

        // when
        List<DepositHistory> depositHistories = depositHistoryRepository
                .findAllByCohortMemberIdOrderByCreatedAtAsc(adminCohortMember.getId());

        // then
        assertThat(cohortRepository.count()).isEqualTo(2);
        assertThat(cohort10.getGeneration()).isEqualTo(10);
        assertThat(cohort11.getGeneration()).isEqualTo(11);
        assertThat(cohort11.isCurrentOperating()).isTrue();

        assertThat(partRepository.findAllByCohortIdOrderByIdAsc(cohort10.getId())).hasSize(PartType.values().length);
        assertThat(partRepository.findAllByCohortIdOrderByIdAsc(cohort11.getId())).hasSize(PartType.values().length);
        assertThat(teamRepository.findAllByCohortIdOrderByIdAsc(cohort11.getId())).hasSize(3);

        assertThat(admin.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(adminCohortMember.getDepositBalance()).isEqualTo(100_000);
        assertThat(adminCohortMember.getPart().getType()).isEqualTo(PartType.SERVER);
        assertThat(adminCohortMember.getTeam().getName()).isEqualTo("Team A");

        assertThat(depositHistories).hasSize(1);
        assertThat(depositHistories.getFirst().getType()).isEqualTo(DepositType.INITIAL);
        assertThat(depositHistories.getFirst().getAmount()).isEqualTo(100_000);
    }

    @Test
    void 시드초기화_재실행시_중복생성되지_않는다() throws Exception {
        // given
        Cohort cohort11 = cohortRepository.findByName("11기").orElseThrow();
        Member admin = memberRepository.findByLoginId("admin").orElseThrow();
        CohortMember adminCohortMember = cohortMemberRepository
                .findByCohortIdAndMemberId(cohort11.getId(), admin.getId())
                .orElseThrow();

        long cohortCountBefore = cohortRepository.count();
        int parts10Before = partRepository.findAllByCohortIdOrderByIdAsc(
                cohortRepository.findByName("10기").orElseThrow().getId()
        ).size();
        int parts11Before = partRepository.findAllByCohortIdOrderByIdAsc(cohort11.getId()).size();
        int teams11Before = teamRepository.findAllByCohortIdOrderByIdAsc(cohort11.getId()).size();
        long memberCountBefore = memberRepository.count();
        long cohortMemberCountBefore = cohortMemberRepository.count();
        int depositHistoryBefore = depositHistoryRepository
                .findAllByCohortMemberIdOrderByCreatedAtAsc(adminCohortMember.getId())
                .size();
        int depositBalanceBefore = adminCohortMember.getDepositBalance();

        // when
        seedDataInitializer.run(new DefaultApplicationArguments(new String[]{}));

        // then
        CohortMember adminCohortMemberAfter = cohortMemberRepository
                .findByCohortIdAndMemberId(cohort11.getId(), admin.getId())
                .orElseThrow();

        assertThat(cohortRepository.count()).isEqualTo(cohortCountBefore);
        assertThat(partRepository.findAllByCohortIdOrderByIdAsc(
                cohortRepository.findByName("10기").orElseThrow().getId()
        )).hasSize(parts10Before);
        assertThat(partRepository.findAllByCohortIdOrderByIdAsc(cohort11.getId())).hasSize(parts11Before);
        assertThat(teamRepository.findAllByCohortIdOrderByIdAsc(cohort11.getId())).hasSize(teams11Before);
        assertThat(memberRepository.count()).isEqualTo(memberCountBefore);
        assertThat(cohortMemberRepository.count()).isEqualTo(cohortMemberCountBefore);
        assertThat(adminCohortMemberAfter.getDepositBalance()).isEqualTo(depositBalanceBefore);
        assertThat(
                depositHistoryRepository.findAllByCohortMemberIdOrderByCreatedAtAsc(adminCohortMemberAfter.getId())
        ).hasSize(depositHistoryBefore);
    }
}
