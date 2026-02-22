package com.prography.backend.domain.cohort.dto;

import com.prography.backend.domain.cohort.entity.Cohort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CohortResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortListDTO {
        private Long id;
        private Integer generation;
        private String name;
        private LocalDateTime createdAt;

        public static CohortListDTO from(Cohort cohort) {
            return CohortListDTO.builder()
                    .id(cohort.getId())
                    .generation(cohort.getGeneration())
                    .name(cohort.getName())
                    .createdAt(cohort.getCreatedAt())
                    .build();
        }
    }
}
