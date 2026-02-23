package com.prography.backend.domain.session.repository;

import com.prography.backend.domain.session.entity.ClubSession;
import com.prography.backend.global.common.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubSessionRepository extends JpaRepository<ClubSession, Long> {

    List<ClubSession> findAllByCohortIdOrderByStartsAtAsc(Long cohortId);
}
