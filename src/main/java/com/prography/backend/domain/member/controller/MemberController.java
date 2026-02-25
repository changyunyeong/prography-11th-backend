package com.prography.backend.domain.member.controller;

import com.prography.backend.domain.member.dto.MemberRequestDTO;
import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.service.MemberService;
import com.prography.backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "member API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/auth/login")
    @Operation(summary = "로그인", description = "loginId와 password로 회원 인증을 수행합니다. 토큰을 발급하지 않으며, 비밀번호 검증 결과만 반환합니다.")
    public ApiResponse<MemberResponseDTO.MemberResultDTO> login(
            @Valid @RequestBody MemberRequestDTO.LoginRequestDTO request
            ) {
        MemberResponseDTO.MemberResultDTO response = memberService.login(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/members/{id}")
    @Operation(summary = "회원 조회", description = "회원의 기본 정보를 조회합니다.")
    public ApiResponse<MemberResponseDTO.MemberResultDTO> getMemberInfo(@PathVariable("id") Long memberId) {
        MemberResponseDTO.MemberResultDTO response = memberService.getMemberInfo(memberId);
        return ApiResponse.success(response);
    }
}
