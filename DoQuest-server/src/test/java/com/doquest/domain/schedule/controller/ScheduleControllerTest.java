package com.doquest.domain.schedule.controller;

import com.doquest.domain.schedule.dto.ScheduleCompletionRequest;
import com.doquest.domain.schedule.dto.ScheduleResponse;
import com.doquest.domain.schedule.dto.ScheduleUpdateRequest;
import com.doquest.domain.schedule.service.ScheduleService;
import com.doquest.global.config.security.JwtAuthenticationFilter;
import com.doquest.global.config.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private void setMockAuthentication(Long memberId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("일정 단건 조회 성공 - 200 OK")
    void getSchedule_Success() throws Exception {
        Long memberId = 1L;
        Long scheduleId = 100L;
        setMockAuthentication(memberId);
        ScheduleResponse response = response(scheduleId, "단건 일정", false);
        given(scheduleService.getSchedule(memberId, scheduleId)).willReturn(response);

        mockMvc.perform(get("/api/v1/schedules/{scheduleId}", scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.title").value("단건 일정"));
    }

    @Test
    @DisplayName("일정 수정 성공 - 200 OK")
    void updateSchedule_Success() throws Exception {
        Long memberId = 1L;
        Long scheduleId = 100L;
        setMockAuthentication(memberId);
        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                "수정 일정", LocalDate.of(2026, 9, 1), LocalTime.of(19, 30), "강남", "수정 요약"
        );
        given(scheduleService.updateSchedule(memberId, scheduleId, request))
                .willReturn(new ScheduleResponse(
                        scheduleId, null, request.title(), request.scheduledAt(),
                        request.scheduledTime(), request.location(), request.summaryInfo(), false
                ));

        mockMvc.perform(patch("/api/v1/schedules/{scheduleId}", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 일정"))
                .andExpect(jsonPath("$.scheduledAt").value("2026-09-01"))
                .andExpect(jsonPath("$.scheduledTime").value("19:30"));
    }

    @Test
    @DisplayName("일정 완료 상태 변경 성공 - 200 OK")
    void changeCompletion_Success() throws Exception {
        Long memberId = 1L;
        Long scheduleId = 100L;
        setMockAuthentication(memberId);
        ScheduleCompletionRequest request = new ScheduleCompletionRequest(true);
        given(scheduleService.changeCompletion(memberId, scheduleId, true))
                .willReturn(response(scheduleId, "완료 일정", true));

        mockMvc.perform(patch("/api/v1/schedules/{scheduleId}/completion", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    @Test
    @DisplayName("일정 완료 상태 누락 - 400 Bad Request")
    void changeCompletion_MissingValue_BadRequest() throws Exception {
        setMockAuthentication(1L);

        mockMvc.perform(patch("/api/v1/schedules/{scheduleId}/completion", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private ScheduleResponse response(Long scheduleId, String title, boolean completed) {
        return new ScheduleResponse(
                scheduleId, null, title, LocalDate.of(2026, 8, 28),
                "서울", "요약", completed
        );
    }
}
