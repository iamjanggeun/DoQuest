package com.doquest.domain.memo.api;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.dto.MemoCreateRequest;
import com.doquest.domain.memo.dto.MemoUpdateRequest;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.service.MemoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemoController.class)
class MemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean // Spring Boot 3.4+ 규격 (이전 버전의 경우 @MockBean 사용)
    private MemoService memoService;

    @Test
    @DisplayName("정상 요청 시 메모가 생성되고 201 Created 응답을 반환한다")
    void createMemo_성공() throws Exception {
        // given
        Long memberId = 1L;
        MemoCreateRequest request = new MemoCreateRequest("내일 알고리즘 스터디 준비");

        given(memoService.createMemo(eq(memberId), any(String.class))).willReturn(100L);

        // when & then
        mockMvc.perform(post("/api/v1/memos")
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.content").value("내일 알고리즘 스터디 준비"))
                .andExpect(jsonPath("$.isParsed").value(false));

        verify(memoService).createMemo(memberId, "내일 알고리즘 스터디 준비");
    }

    @Test // 500 Error 반환하여 핸들러 수정
    @DisplayName("메모 내용이 공백인 경우 @Valid 검증에 실패하여 400 Bad Request를 반환한다")
    void createMemo_공백_검증_실패() throws Exception {
        // given
        Long memberId = 1L;
        MemoCreateRequest request = new MemoCreateRequest("   "); // Blank 요청

        // when & then
        mockMvc.perform(post("/api/v1/memos")
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원의 메모 목록 조회 시 200 OK와 MemoResponse 리스트를 반환한다")
    void getMemos_성공() throws Exception {
        // given
        Long memberId = 1L;
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Memo memo1 = Memo.createMemo(member, "메모 1");
        Memo memo2 = Memo.createMemo(member, "메모 2");

        given(memoService.getMemosByMemberId(memberId)).willReturn(List.of(memo2, memo1));

        // when & then
        mockMvc.perform(get("/api/v1/memos")
                        .header("X-Member-Id", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("메모 2"))
                .andExpect(jsonPath("$[1].content").value("메모 1"));
    }

    @Test
    @DisplayName("메모 수정 요청 시 200 OK를 반환한다")
    void updateMemo_성공() throws Exception {
        // given
        Long memberId = 1L;
        Long memoId = 10L;
        MemoUpdateRequest request = new MemoUpdateRequest("수정된 메모 내용");

        // when & then
        mockMvc.perform(patch("/api/v1/memos/{memoId}", memoId)
                        .header("X-Member-Id", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(memoService).updateMemo(memberId, memoId, "수정된 메모 내용");
    }

    @Test
    @DisplayName("메모 삭제 요청 시 200 OK를 반환한다")
    void deleteMemo_성공() throws Exception {
        // given
        Long memberId = 1L;
        Long memoId = 10L;

        // when & then
        mockMvc.perform(delete("/api/v1/memos/{memoId}", memoId)
                        .header("X-Member-Id", memberId))
                .andExpect(status().isOk());

        verify(memoService).deleteMemo(memberId, memoId);
    }
}