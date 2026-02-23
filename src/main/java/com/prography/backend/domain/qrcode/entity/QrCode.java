package com.prography.backend.domain.qrcode.entity;

import com.prography.backend.global.common.base.BaseEntity;
import com.prography.backend.domain.session.entity.ClubSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "qr_codes")
public class QrCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClubSession session;

    @Column(name = "hash_value", nullable = false, length = 64)
    private String hashValue;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

}
