package com.prography.backend.domain.auth.dto;

import com.prography.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuthResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResultDTO {
        private Long id;
        private String loginId;
        private String name;
        private String phone;
        private String status;
        private String role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static LoginResultDTO from(Member member) {
            return LoginResultDTO.builder()
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
}
