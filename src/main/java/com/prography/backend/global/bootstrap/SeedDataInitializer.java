package com.prography.backend.global.bootstrap;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.cohort.repository.CohortMemberRepository;
import com.prography.backend.domain.cohort.repository.CohortRepository;
import com.prography.backend.domain.cohort.repository.PartRepository;
import com.prography.backend.domain.cohort.repository.TeamRepository;
import com.prography.backend.domain.member.repository.MemberRepository;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.domain.deposit.service.DepositService;
import com.prography.backend.global.common.enums.PartType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private static final int INITIAL_DEPOSIT = 100_000;

    private final CohortRepository cohortRepository;
    private final PartRepository partRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepositService depositService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Cohort cohort10 = getOrCreateCohort(10, "10기", false);
        Cohort cohort11 = getOrCreateCohort(11, "11기", true);

        createPartsIfAbsent(cohort10);
        Map<PartType, Part> parts11 = createPartsIfAbsent(cohort11);

        Team teamA = getOrCreateTeam(cohort11, "Team A");
        getOrCreateTeam(cohort11, "Team B");
        getOrCreateTeam(cohort11, "Team C");

        Member admin = memberRepository.findByLoginId("admin")
            .orElseGet(() -> memberRepository.save(
                Member.builder()
                    .loginId("admin")
                    .password(passwordEncoder.encode("admin1234"))
                    .name("관리자")
                    .phone(null)
                    .role(MemberRole.ADMIN)
                    .status(MemberStatus.ACTIVE)
                    .build()
            ));

        CohortMember adminCohortMember = cohortMemberRepository.findByCohortIdAndMemberId(cohort11.getId(), admin.getId())
            .orElseGet(() -> cohortMemberRepository.save(
                CohortMember.builder()
                    .cohort(cohort11)
                    .member(admin)
                    .part(parts11.get(PartType.SERVER))
                    .team(teamA)
                    .depositBalance(0)
                    .excuseCount(0)
                    .build()
            ));

        if (adminCohortMember.getDepositBalance() == 0) {
            depositService.initializeDeposit(adminCohortMember, INITIAL_DEPOSIT, "초기 보증금 설정");
        }
    }

    private Cohort getOrCreateCohort(int generation, String name, boolean currentOperating) {
        return cohortRepository.findByName(name)
            .orElseGet(() -> cohortRepository.save(
                Cohort.builder()
                    .generation(generation)
                    .name(name)
                    .currentOperating(currentOperating)
                    .build()
            ));
    }

    private Map<PartType, Part> createPartsIfAbsent(Cohort cohort) {
        Map<PartType, Part> parts = new EnumMap<>(PartType.class);

        for (PartType partType : PartType.values()) {
            Part part = partRepository.findByCohortIdAndType(cohort.getId(), partType)
                .orElseGet(() -> partRepository.save(
                    Part.builder()
                        .cohort(cohort)
                        .type(partType)
                        .build()
                ));
            parts.put(partType, part);
        }

        return parts;
    }

    private Team getOrCreateTeam(Cohort cohort, String teamName) {
        return teamRepository.findByCohortIdAndName(cohort.getId(), teamName)
            .orElseGet(() -> teamRepository.save(
                Team.builder()
                    .cohort(cohort)
                    .name(teamName)
                    .build()
            ));
    }
}
