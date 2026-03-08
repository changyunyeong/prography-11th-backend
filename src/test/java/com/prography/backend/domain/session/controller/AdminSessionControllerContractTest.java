package com.prography.backend.domain.session.controller;

import com.prography.backend.domain.session.dto.SessionResponseDTO;
import com.prography.backend.domain.session.service.AdminSessionService;
import com.prography.backend.global.common.enums.SessionStatus;
import com.prography.backend.global.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSessionController.class)
@Import(GlobalExceptionHandler.class)
class AdminSessionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminSessionService adminSessionService;

    @Test
    void 일정생성성공응답은_201과_공통포맷을_반환한다() throws Exception {
        // given
        SessionResponseDTO.SessionResultDTO response = SessionResponseDTO.SessionResultDTO.builder()
                .id(1L)
                .cohortId(2L)
                .title("정기 모임")
                .date(LocalDate.of(2026, 3, 1))
                .time(LocalTime.of(14, 0))
                .location("강남")
                .status(SessionStatus.SCHEDULED)
                .attendanceSummary(SessionResponseDTO.AttendanceSummaryDTO.builder()
                        .present(0)
                        .absent(0)
                        .late(0)
                        .excused(0)
                        .total(0)
                        .build())
                .qrActive(true)
                .createdAt(Instant.parse("2026-03-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-03-01T00:00:00Z"))
                .build();
        when(adminSessionService.createSession(any())).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/admin/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "정기 모임",
                                  "date": "2026-03-01",
                                  "time": "14:00",
                                  "location": "강남"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.qrActive").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void 일정생성검증실패응답은_INVALID_INPUT_포맷을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/admin/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "정기 모임",
                                  "date": "2026-03-01",
                                  "time": "99:99",
                                  "location": "강남"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
