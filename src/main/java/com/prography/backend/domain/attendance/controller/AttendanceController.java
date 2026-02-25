package com.prography.backend.domain.attendance.controller;

import com.prography.backend.domain.attendance.dto.AttendanceRequestDTO;
import com.prography.backend.domain.attendance.dto.AttendanceResponseDTO;
import com.prography.backend.domain.attendance.service.AttendanceService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Attendance", description = "Attendance API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "QR 출석 체크", description = "QR 코드의 hashValue와 memberId를 전송하여 출석 체크를 수행합니다.")
    public ApiResponse<AttendanceResponseDTO.AttendanceResultDTO> checkAttendance(
            @Valid @RequestBody AttendanceRequestDTO.CheckAttendanceRequestDTO request
    ) {
        AttendanceResponseDTO.AttendanceResultDTO response = attendanceService.checkAttendance(request);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "내 출결 기록 조회", description = "특정 회원의 전체 출결 기록을 조회합니다.")
    public ApiResponse<List<AttendanceResponseDTO.AttendanceHistoryDTO>> getAttendances(
            @RequestParam("memberId") Long memberId
    ) {
        List<AttendanceResponseDTO.AttendanceHistoryDTO> response = attendanceService.getAttendances(memberId);
        return ApiResponse.success(response);
    }
}
