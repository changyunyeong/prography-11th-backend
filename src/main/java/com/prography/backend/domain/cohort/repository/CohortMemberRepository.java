package com.prography.backend.domain.cohort.repository;

import com.prography.backend.domain.cohort.entity.CohortMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CohortMemberRepository extends JpaRepository<CohortMember, Long> {
    Optional<CohortMember> findByCohortIdAndMemberId(Long cohortId, Long memberId);
    @EntityGraph(attributePaths = {"cohort", "part", "team", "member"}) // 연관 엔티티를 같이 로딩하여 N+1 문제 방지
    List<CohortMember> findAllByMemberIdIn(List<Long> memberIds);
}
