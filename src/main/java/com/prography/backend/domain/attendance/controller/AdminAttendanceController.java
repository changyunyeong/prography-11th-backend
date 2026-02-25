package com.prography.backend.domain.attendance.controller;

import com.prography.backend.domain.attendance.dto.AttendanceRequestDTO;
import com.prography.backend.domain.attendance.dto.AttendanceResponseDTO;
import com.prography.backend.domain.attendance.service.AdminAttendanceService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Attendance", description = "admin Attendance API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/attendances")
public class AdminAttendanceController {

    private final AdminAttendanceService adminAttendanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "출결 등록", description = "관리자가 수동으로 출결을 등록합니다.")
    public ApiResponse<AttendanceResponseDTO.AttendanceResultDTO> registerAttendance(
            @Valid @RequestBody AttendanceRequestDTO.RegisterAttendanceRequestDTO request
    ) {
        AttendanceResponseDTO.AttendanceResultDTO response = adminAttendanceService.registerAttendance(request);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "출결 수정", description = "기존 출결의 상태를 변경하고, 패널티 차이에 따라 보증금을 자동 조정합니다.")
    public ApiResponse<AttendanceResponseDTO.AttendanceResultDTO> updateAttendance(
            @Valid @RequestBody AttendanceRequestDTO.UpdateAttendanceRequestDTO request,
            @PathVariable("id") Long attendanceId
    ) {
        AttendanceResponseDTO.AttendanceResultDTO response = adminAttendanceService.updateAttendance(request, attendanceId);
        return ApiResponse.success(response);
    }

    @GetMapping("/sessions/{sessionId}/summary")
    @Operation(summary = "일정별 회원 출결 요약", description = "해당 기수(11기) 전체 회원의 출결 통계를 조회합니다.")
    public ApiResponse<List<AttendanceResponseDTO.SessionAttendanceSummaryDTO>> getSessionAttendanceSummary(
            @PathVariable("sessionId") Long sessionId
    ) {
        List<AttendanceResponseDTO.SessionAttendanceSummaryDTO> response =
                adminAttendanceService.getSessionAttendanceSummary(sessionId);
        return ApiResponse.success(response);
    }

    @GetMapping("/members/{memberId}")
    @Operation(summary = "회원 출결 상세", description = "특정 회원의 기수/파트/팀 정보 + 전체 출결 기록 + 보증금/공결 정보를 조회합니다.")
    public ApiResponse<AttendanceResponseDTO.AttendanceDetailDTO> getAttendanceDetail(@PathVariable("memberId") Long memberId) {
        AttendanceResponseDTO.AttendanceDetailDTO response = adminAttendanceService.getAttendanceDetail(memberId);
        return ApiResponse.success(response);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "일정별 출결 목록", description = "출결 목록을 페이징, 필터링, 검색 조건으로 조회합니다특정 일정에 등록된 전체 출결 기록을 조회합니다.")
    public ApiResponse<AttendanceResponseDTO.SessionAttendanceDetailDTO> getSessionAttendanceDetail(
            @PathVariable("sessionId") Long sessionId
    ) {
        AttendanceResponseDTO.SessionAttendanceDetailDTO response =
                adminAttendanceService.getSessionAttendanceDetail(sessionId);
        return ApiResponse.success(response);
    }
}
