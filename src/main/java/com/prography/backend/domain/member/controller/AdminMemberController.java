package com.prography.backend.domain.member.controller;

import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.domain.member.service.MemberService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Member", description = "admin member API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "회원 등록", description = "신규 회원을 등록하고, 기수에 배정하며, 보증금을 초기화합니다.")
    public ApiResponse<MemberResponseDTO.MemberResultDTO> createMember(@Valid @RequestBody MemberRequestDTO.CreateMemberRequestDTO request) {

        MemberResponseDTO.MemberResultDTO response = memberService.createMember(request);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "회원 대시보드 조회 목록 조회", description = "회원 목록을 페이징, 필터링, 검색 조건으로 조회합니다. \n" +
            " - 검색 유형: name, loginId, phone \n")
    public ApiResponse<MemberResponseDTO.MemberDashboardPreViewListDTO> getMemberDashboard(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "searchValue", required = false) String searchValue,
            @RequestParam(value = "generation", required = false) Integer generation,
            @RequestParam(value = "partName", required = false) String partName,
            @RequestParam(value = "teamName", required = false) String teamName,
            @RequestParam(value = "status", required = false) MemberStatus status
            ) {
        MemberResponseDTO.MemberDashboardPreViewListDTO response = memberService.getMemberDashboard(
                page, size, searchType, searchValue, generation, partName, teamName, status
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "회원 상세 조회", description = "회원의 상세 정보를 기수/파트/팀 정보와 함께 조회합니다.")
    public ApiResponse<MemberResponseDTO.MemberResultDTO> getMemberDetail(@PathVariable("id") Long memberId) {
        MemberResponseDTO.MemberResultDTO response = memberService.getMemberDetail(memberId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "회원 수정", description = "회원 정보를 수정합니다. 모든 필드는 optional이며, 전달된 필드만 수정됩니다.")
    public ApiResponse<MemberResponseDTO.MemberResultDTO> updateMember(@PathVariable("id") Long memberId,
                                                        @Valid @RequestBody MemberRequestDTO.UpdateMemberRequestDTO request) {
        MemberResponseDTO.MemberResultDTO response = memberService.updateMember(memberId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "회원 탈퇴", description = "회원 정보를 삭제합니다. 회원을 Soft-delete 처리합니다. 실제 삭제가 아닌 상태를 WITHDRAWN으로 변경합니다.")
    public ApiResponse<MemberResponseDTO.MemberDeleteDTO> deleteMember(@PathVariable("id") Long memberId) {
        MemberResponseDTO.MemberDeleteDTO response = memberService.deleteMember(memberId);
        return ApiResponse.success(response);
    }
}
