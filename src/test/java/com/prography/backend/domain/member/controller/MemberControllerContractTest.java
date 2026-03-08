package com.prography.backend.domain.member.controller;

import com.prography.backend.domain.member.dto.MemberResponseDTO;
import com.prography.backend.domain.member.service.MemberService;
import com.prography.backend.global.common.enums.MemberRole;
import com.prography.backend.global.common.enums.MemberStatus;
import com.prography.backend.global.common.error.ApiException;
import com.prography.backend.global.common.error.ErrorCode;
import com.prography.backend.global.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import(GlobalExceptionHandler.class)
class MemberControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Test
    void 로그인성공응답은_공통포맷을_따른다() throws Exception {
        // given
        MemberResponseDTO.MemberResultDTO response = MemberResponseDTO.MemberResultDTO.builder()
                .id(1L)
                .loginId("admin")
                .name("관리자")
                .phone("010-0000-0000")
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.ADMIN)
                .createdAt(Instant.parse("2026-03-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-03-01T00:00:00Z"))
                .build();
        when(memberService.login(any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "admin",
                                  "password": "admin1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.loginId").value("admin"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void 검증실패응답은_INVALID_INPUT_포맷을_따른다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message", not(isEmptyOrNullString())));
    }

    @Test
    void ApiException은_명세코드와_HTTP상태로_매핑된다() throws Exception {
        // given
        when(memberService.getMemberInfo(404L)).thenThrow(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/members/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
    }

    @Test
    void 예상치못한예외도_공통에러포맷으로_반환된다() throws Exception {
        // given
        when(memberService.getMemberInfo(1L)).thenThrow(new RuntimeException("db down"));

        // when & then
        mockMvc.perform(get("/api/v1/members/{id}", 1L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
