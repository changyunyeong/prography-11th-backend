package com.prography.backend.domain.dto.member;

import com.prography.backend.domain.entity.cohort.CohortMember;
import com.prography.backend.domain.entity.member.Member;
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
    public static class MemberInfoDTO {
        private Long id;
        private String loginId;
        private String name;
        private String phone;
        private String status;
        private String role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static MemberInfoDTO from(Member member) {
            return MemberInfoDTO.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .phone(member.getPhone())
                .status(member.getStatus().name())
                .role(member.getRole().name())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        }
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDetailDTO {
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

        public static MemberDetailDTO from(Member member, CohortMember cohortMember) {
            Integer generation = null;
            String partName = null;
            String teamName = null;

            if (cohortMember != null) {
                generation = extractGeneration(cohortMember.getCohort().getName());
                partName = cohortMember.getPart() != null ? cohortMember.getPart().getType().name() : null;
                teamName = cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null;
            }

            return MemberDetailDTO.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .phone(member.getPhone())
                .status(member.getStatus().name())
                .role(member.getRole().name())
                .generation(generation)
                .partName(partName)
                .teamName(teamName)
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        }
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardMemberDTO {
        private Long id;
        private String loginId;
        private String name;
        private String phone;
        private Integer generation;
        private String partName;
        private String teamName;
        private String role;
        private String status;
        private int deposit;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static DashboardMemberDTO from(CohortMember cohortMember) {
            Member member = cohortMember.getMember();
            return DashboardMemberDTO.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .phone(member.getPhone())
                .generation(extractGeneration(cohortMember.getCohort().getName()))
                .partName(cohortMember.getPart() != null ? cohortMember.getPart().getType().name() : null)
                .teamName(cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null)
                .role(member.getRole().name())
                .status(member.getStatus().name())
                .deposit(cohortMember.getDepositBalance())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        }
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardDTO {
        private List<DashboardMemberDTO> content;
        private int page;
        private int size;
        private int totalElements;
        private int totalPages;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummaryDTO {
        private Long memberId;
        private long present;
        private long absent;
        private long late;
        private long excused;
        private int totalPenalty;
        private Integer deposit;
    }

    private static Integer extractGeneration(String cohortName) {
        if (cohortName == null) {
            return null;
        }
        try {
            return Integer.parseInt(cohortName.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
