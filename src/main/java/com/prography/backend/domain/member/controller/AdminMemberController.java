package com.prography.backend.domain.member.controller;

import com.prography.backend.domain.cohort.entity.CohortMember;
import com.prography.backend.domain.member.entity.Member;
import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.response.ApiResponse;
import com.prography.backend.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Member", description = "admin member API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "회원 등록", description = "신규 회원을 등록하고, 기수에 배정하며, 보증금을 초기화합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponseDTO.MemberCreateResponseDTO> createMember(@Valid @RequestBody MemberRequestDTO.CreateMemberRequestDTO request) {

        MemberResponseDTO.MemberCreateResponseDTO response = memberService.createMember(request);
        return ApiResponse.success(response);
    }

}
