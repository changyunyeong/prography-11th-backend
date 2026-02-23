package com.prography.backend.domain.qrcode.repository;

import com.prography.backend.domain.qrcode.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    @Query("select q from QrCode q where q.session.id = :sessionId and q.revokedAt is null and q.expiresAt > :now")
    List<QrCode> findActiveBySessionId(Long sessionId, LocalDateTime now);

    @Query("select q from QrCode q where q.session.id in :sessionIds and q.revokedAt is null and q.expiresAt > :now")
    List<QrCode> findActiveBySessionIdIn(List<Long> sessionIds, LocalDateTime now);

}
