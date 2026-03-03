package com.prography.backend.domain.session.entity;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.global.common.base.BaseEntity;
import com.prography.backend.global.common.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sessions")
public class ClubSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    public void updateSession(String title, LocalDateTime startsAt, String location, SessionStatus status) {
        if (title != null) {
            this.title = title;
        }
        if (startsAt != null) {
            this.startsAt = startsAt;
        }
        if (location != null) {
            this.location = location;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void updateStatus(SessionStatus status) {
        this.status = status;
    }
}
