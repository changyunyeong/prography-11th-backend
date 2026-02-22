package com.prography.backend.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class MemberRequestDTO {

    @Getter
    public static class CreateMemberRequestDTO {

        @NotBlank
        private String loginId;
        @NotBlank
        private String password;
        @NotBlank
        private String name;
        @NotBlank
        private String phone;
        @NotNull
        private Long cohortId;
        private Long partId;
        private Long teamId;
    }

    @Getter
    public static class UpdateMemberRequestDTO {

        private String name;
        private String phone;
        private Long cohortId;
        private Long partId;
        private Long teamId;
    }
}
