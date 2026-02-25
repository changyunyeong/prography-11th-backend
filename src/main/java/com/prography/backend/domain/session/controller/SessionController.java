package com.prography.backend.domain.session.controller;

import com.prography.backend.domain.session.dto.SessionResponseDTO;
import com.prography.backend.domain.session.service.SessionService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Sessions", description = "sessions API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "일정 목록 조회 (회원용)", description = "현재 기수(11기)의 일정 목록을 조회합니다. CANCELLED 상태의 일정은 제외됩니다.")
    public ApiResponse<List<SessionResponseDTO.SessionInfoDTO>> getSessionInfo() {
        List<SessionResponseDTO.SessionInfoDTO> response = sessionService.getSessionList();
        return ApiResponse.success(response);
    }
}
