package com.prography.backend.domain.session.dto;

import com.prography.backend.global.common.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.time.LocalDate;

public class SessionRequestDTO {

    @Getter
    public static class CreateSessionRequestDTO {

        @NotBlank
        private String title;
        @NotNull
        private LocalDate date;

        @NotBlank
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
        @Schema(type = "string", example = "14:00", description = "HH:mm 형식")
        private String time;

        @NotBlank
        private String location;
    }
}
