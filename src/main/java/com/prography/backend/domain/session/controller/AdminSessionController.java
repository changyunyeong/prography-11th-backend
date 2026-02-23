package com.prography.backend.domain.session.controller;

import com.prography.backend.domain.session.dto.SessionRequestDTO;
import com.prography.backend.domain.session.dto.SessionResponseDTO;
import com.prography.backend.domain.session.service.SessionService;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Admin Sessions", description = "admin sessions API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sessions")
public class AdminSessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "일정 생성", description = "새 일정을 생성합니다. QR 코드가 자동으로 함께 생성됩니다.")
    public ApiResponse<SessionResponseDTO.SessionResultDTO> createSession(
            @Valid @RequestBody SessionRequestDTO.CreateSessionRequestDTO request
    ) {
        SessionResponseDTO.SessionResultDTO response = sessionService.createSession(request);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "일정 목록 조회 (관리자용)", description = "현재 기수(11기)의 일정 목록을 출결 요약 정보와 함께 조회합니다.")
    public ApiResponse<List<SessionResponseDTO.SessionResultDTO>> getAdminSessionList(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "status", required = false) SessionStatus status
    ) {
        List<SessionResponseDTO.SessionResultDTO> response = sessionService.getSessionList(dateFrom, dateTo, status);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "일정 수정", description = "일정 정보를 수정합니다. 모든 필드는 optional이며, 전달된 필드만 수정됩니다.")
    public ApiResponse<SessionResponseDTO.SessionResultDTO> updateSession(
            @PathVariable("id") Long sessionId,
            @Valid @RequestBody SessionRequestDTO.UpdateSessionRequestDTO request
    ) {
        SessionResponseDTO.SessionResultDTO response = sessionService.updateSession(sessionId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "일정 삭제", description = "일정을 Soft-delete 처리합니다. 실제 삭제가 아닌 상태를 CANCELLED로 변경합니다.")
    public ApiResponse<SessionResponseDTO.SessionResultDTO> deleteSession(
            @PathVariable("id") Long sessionId
    ) {
        SessionResponseDTO.SessionResultDTO response = sessionService.deleteSession(sessionId);
        return ApiResponse.success(response);
    }
}
