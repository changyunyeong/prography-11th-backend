package com.prography.backend.domain.member.dto;

import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class MemberResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResultDTO {
        private Long id;
        private String loginId;
        private String name;
        private String phone;
        private String status;
        private String role;
        private Integer generation;
        private String partName;
        private String teamName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static MemberResultDTO from(Member member, CohortMember cohortMember) {
            return MemberResultDTO.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .phone(member.getPhone())
                .status(member.getStatus().name())
                .role(member.getRole().name())
                .generation(cohortMember != null ? cohortMember.getCohort().getGeneration() : null)
                .partName(cohortMember != null && cohortMember.getPart() != null ? cohortMember.getPart().getType().name() : null)
                .teamName(cohortMember != null && cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null)
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDashboardPreViewDTO {
        private Long id;
        private String loginId;
        private String name;
        private String phone;
        private String status;
        private String role;
        private Integer generation;
        private String partName;
        private String teamName;
        private Integer deposit;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static MemberDashboardPreViewDTO from(Member member, CohortMember cohortMember) {
            return MemberDashboardPreViewDTO.builder()
                    .id(member.getId())
                    .loginId(member.getLoginId())
                    .name(member.getName())
                    .phone(member.getPhone())
                    .status(member.getStatus().name())
                    .role(member.getRole().name())
                    .generation(cohortMember != null ? cohortMember.getCohort().getGeneration() : null)
                    .partName(cohortMember != null && cohortMember.getPart() != null ? cohortMember.getPart().getType().name() : null)
                    .teamName(cohortMember != null && cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null)
                    .deposit(cohortMember != null ? cohortMember.getDepositBalance() : null)
                    .createdAt(member.getCreatedAt())
                    .updatedAt(member.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDashboardPreViewListDTO {
        private List<MemberDashboardPreViewDTO> content;
        private int page;
        private int size;
        private Long totalElements;
        private Integer totalPages;
    }
}
