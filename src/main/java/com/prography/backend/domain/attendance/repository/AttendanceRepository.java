package com.prography.backend.domain.attendance.repository;

import com.prography.backend.domain.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findAllBySessionIdOrderByCheckedAtAsc(Long sessionId);
    List<Attendance> findAllBySessionIdIn(List<Long> sessionIds);
    List<Attendance> findAllByCohortMemberIdIn(List<Long> cohortMemberIds);
    List<Attendance> findAllByMemberIdOrderByCreatedAtAsc(Long memberId);
    boolean existsBySessionIdAndMemberId(Long sessionId, Long memberId);
}
