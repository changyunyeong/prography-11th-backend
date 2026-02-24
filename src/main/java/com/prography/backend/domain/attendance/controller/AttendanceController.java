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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Attendance", description = "admin Attendance API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @Operation(summary = "출결 등록", description = "관리자가 수동으로 출결을 등록합니다.")
    public ApiResponse<AttendanceResponseDTO.AttendanceResultDTO> registerAttendance(
            @Valid @RequestBody AttendanceRequestDTO.RegisterAttendanceRequestDTO request
    ) {
        AttendanceResponseDTO.AttendanceResultDTO result = attendanceService.registerAttendance(request);
        return ApiResponse.success(result);
    }
}
