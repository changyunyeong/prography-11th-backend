package com.prography.backend.domain.member.repository;

import com.prography.backend.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);

    /**
     JpaSpecificationExecutor
     대시보드에서 선택값이 많아 조합이 많아서 정적 메서드로는 관리 힘듦
     -> service에서 Specification<Member>를 만들어 조건이 있을 때만 Predicate를 추가해서 동적 쿼리 생성
     * */
}
