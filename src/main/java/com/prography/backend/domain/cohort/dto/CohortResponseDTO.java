package com.prography.backend.domain.cohort.dto;

import com.prography.backend.domain.cohort.entity.Cohort;
import com.prography.backend.domain.cohort.entity.Part;
import com.prography.backend.domain.cohort.entity.Team;
import com.prography.backend.domain.deposit.entity.DepositHistory;
import com.prography.backend.global.common.enums.DepositType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class CohortResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortListDTO {
        private Long id;
        private Integer generation;
        private String name;
        private Instant createdAt;

        public static CohortListDTO from(Cohort cohort) {
            return CohortListDTO.builder()
                    .id(cohort.getId())
                    .generation(cohort.getGeneration())
                    .name(cohort.getName())
                    .createdAt(cohort.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortDetailDTO {
        private Long id;
        private Integer generation;
        private String name;
        private List<CohortPartDTO> parts;
        private List<CohortTeamDTO> teams;
        private Instant createdAt;

        public static CohortDetailDTO from(Cohort cohort, List<Part> parts, List<Team> teams) {
            return CohortDetailDTO.builder()
                    .id(cohort.getId())
                    .generation(cohort.getGeneration())
                    .name(cohort.getName())
                    .parts(parts.stream()
                            .map(CohortPartDTO::from)
                            .toList())
                    .teams(teams.stream()
                            .map(CohortTeamDTO::from)
                            .toList())
                    .createdAt(cohort.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortPartDTO {
        private Long id;
        private String name;

        public static CohortPartDTO from(Part part) {
            return CohortPartDTO.builder()
                    .id(part.getId())
                    .name(formatPartName(part))
                    .build();
        }

        private static String formatPartName(Part part) {
            if (part.getType() == null) {
                return null;
            }
            if ("IOS".equals(part.getType().name())) {
                return "iOS";
            }
            return part.getType().name();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CohortTeamDTO {
        private Long id;
        private String name;

        public static CohortTeamDTO from(Team team) {
            return CohortTeamDTO.builder()
                    .id(team.getId())
                    .name(team.getName())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepositHistoryDTO {
        private Long id;
        private Long cohortMemberId;
        private DepositType type;
        private Integer amount;
        private Integer balanceAfter;
        private Long attendanceId;
        private String description;
        private Instant createdAt;

        public static DepositHistoryDTO from(DepositHistory deposit) {
            int displayAmount = deposit.getAmount();
            if (deposit.getType() == DepositType.PENALTY) {
                displayAmount = -displayAmount;
            }

            return DepositHistoryDTO.builder()
                    .id(deposit.getId())
                    .cohortMemberId(deposit.getCohortMember().getId())
                    .type(deposit.getType())
                    .amount(displayAmount)
                    .balanceAfter(deposit.getBalanceAfter())
                    .attendanceId(deposit.getAttendance() != null ? deposit.getAttendance().getId() : null)
                    .description(deposit.getDescription())
                    .createdAt(deposit.getCreatedAt())
                    .build();
        }
    }
}
