package com.prography.backend.domain.member.dto;

import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MemberResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberCreateResponseDTO {
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

        public static MemberCreateResponseDTO from(Member member, CohortMember cohortMember) {
            return MemberCreateResponseDTO.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .phone(member.getPhone())
                .status(member.getStatus().name())
                .role(member.getRole().name())
                .generation(cohortMember.getCohort().getGeneration())
                .partName(cohortMember.getPart() != null ? cohortMember.getPart().getType().name() : null)
                .teamName(cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null)
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
        }

    }
}
