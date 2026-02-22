package com.prography.backend.domain.cohort.controller;

import com.prography.backend.domain.cohort.dto.CohortResponseDTO;
import com.prography.backend.domain.cohort.service.CohortService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Cohorts", description = "admin cohorts API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/cohorts")
public class AdminCohortController {

    private final CohortService cohortService;

    @GetMapping
    @Operation(summary = "기수 목록 조회", description = "전체 기수 목록을 조회합니다.")
    public ApiResponse<List<CohortResponseDTO.CohortListDTO>> getCohortList() {
        List<CohortResponseDTO.CohortListDTO> response = cohortService.getCohortList();
        return ApiResponse.success(response);
    }
}
