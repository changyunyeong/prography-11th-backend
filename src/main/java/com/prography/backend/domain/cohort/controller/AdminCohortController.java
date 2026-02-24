package com.prography.backend.domain.cohort.controller;

import com.prography.backend.domain.cohort.dto.CohortResponseDTO;
import com.prography.backend.domain.cohort.service.CohortService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Cohorts", description = "admin cohorts API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminCohortController {

    private final CohortService cohortService;

    @GetMapping("/cohorts")
    @Operation(summary = "기수 목록 조회", description = "전체 기수 목록을 조회합니다.")
    public ApiResponse<List<CohortResponseDTO.CohortListDTO>> getCohortList() {
        List<CohortResponseDTO.CohortListDTO> response = cohortService.getCohortList();
        return ApiResponse.success(response);
    }

    @GetMapping("/cohorts/{cohortId}")
    @Operation(summary = "기수 상세 조회", description = "기수 정보와 소속 파트/팀 목록을 함께 조회합니다.")
    public ApiResponse<CohortResponseDTO.CohortDetailDTO> getCohortDetail(@PathVariable("cohortId") Long cohortId) {
        CohortResponseDTO.CohortDetailDTO response = cohortService.getCohortDetail(cohortId);
        return ApiResponse.success(response);
    }

    @GetMapping("/cohort-members/{cohortMemberId}/deposits")
    @Operation(summary = "보증금 이력 조회", description = "특정 기수 회원의 보증금 변동 이력을 시간순으로 조회합니다.")
    public ApiResponse<List<CohortResponseDTO.DepositHistoryDTO>> getDepositHistory(@PathVariable("cohortMemberId") Long cohortMemberId) {
        List<CohortResponseDTO.DepositHistoryDTO> response = cohortService.getDepositHistory(cohortMemberId);
        return ApiResponse.success(response);
    }
}
