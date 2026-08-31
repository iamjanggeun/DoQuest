package com.doquest.domain.memo.api;

import com.doquest.domain.memo.dto.MemoCreateRequest;
import com.doquest.domain.memo.dto.MemoAnalysisResponse;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.dto.MemoUpdateRequest;
import com.doquest.domain.memo.service.MemoService;
import com.doquest.domain.memo.service.MemoAnalysisService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemoController.class)
@AutoConfigureMockMvc(addFilters = false) // Web 레이어 단위 검증 집중을 위해 Security Filter 격리
class MemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemoService memoService;
    @MockitoBean
    private MemoAnalysisService memoAnalysisService;
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
    @DisplayName("메모 생성 성공 - 201 Created")
    void createMemo_성공() throws Exception {
        // given
        Long memberId = 1L;
        setMockAuthentication(memberId);

        MemoCreateRequest request = new MemoCreateRequest("Spring Security 공부하기");
        given(memoService.createMemo(eq(memberId), eq(request.content()))).willReturn(100L);

        // when & then
        mockMvc.perform(post("/api/v1/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(100L));
    }

    @Test
    @DisplayName("메모 생성 실패 - 빈 내용 입력 시 400 Bad Request (@Valid 검증)")
    void createMemo_공백_검증_실패() throws Exception {
        // given
        Long memberId = 1L;
        setMockAuthentication(memberId);

        MemoCreateRequest request = new MemoCreateRequest("   "); // Blank

        // when & then
        mockMvc.perform(post("/api/v1/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()) // HTTP Status 400 검증
                .andExpect(jsonPath("$.code").value("C001")) // 실제 에러 코드 검증
                .andExpect(jsonPath("$.message").value("적절하지 않은 요청 값입니다."));
    }

    @Test
    @DisplayName("메모 목록 조회 성공 - 200 OK")
    void getMemos_성공() throws Exception {
        // given
        Long memberId = 1L;
        setMockAuthentication(memberId);

        List<MemoResponse> responses = List.of(
                new MemoResponse(1L, "첫 번째 메모", false, Instant.now()),
                new MemoResponse(2L, "두 번째 메모", false, Instant.now())
        );

        given(memoService.getMemosByMemberId(memberId)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/memos"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("첫 번째 메모"))
                .andExpect(jsonPath("$[0].isParsed").value(false));
    }

    @Test
    @DisplayName("메모 수정 성공 - 200 OK")
    void updateMemo_성공() throws Exception {
        // given
        Long memberId = 1L;
        Long memoId = 100L;
        setMockAuthentication(memberId);

        MemoUpdateRequest request = new MemoUpdateRequest("수정된 메모 내용");
        doNothing().when(memoService).updateMemo(eq(memberId), eq(memoId), eq(request.content()));

        // when & then
        mockMvc.perform(patch("/api/v1/memos/{memoId}", memoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AI 일정 찾기 시작 성공 - 202 Accepted")
    void requestAnalysis_성공() throws Exception {
        Long memberId = 1L;
        Long memoId = 100L;
        setMockAuthentication(memberId);
        given(memoAnalysisService.requestAnalysis(memberId, memoId)).willReturn(
                new MemoAnalysisResponse(memoId, MemoAnalysisStatus.PENDING, false,
                        null, (LocalDate) null, null, null)
        );

        mockMvc.perform(post("/api/v1/memos/{memoId}/analysis", memoId))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("메모 삭제 성공 - 204 No Content")
    void deleteMemo_성공() throws Exception {
        // given
        Long memberId = 1L;
        Long memoId = 100L;
        setMockAuthentication(memberId);

        doNothing().when(memoService).deleteMemo(memberId, memoId);

        // when & then
        mockMvc.perform(delete("/api/v1/memos/{memoId}", memoId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
