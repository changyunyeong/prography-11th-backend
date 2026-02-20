package com.prography.backend.domain.dto.member;

import com.prography.backend.global.common.enums.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
