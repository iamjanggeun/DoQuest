package com.doquest.domain.memo.service;

import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.ai.service.AiRetryExecutor;
import com.doquest.domain.memo.event.MemoAnalysisRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemoAiEventListener 단위 테스트")
class MemoAiEventListenerTest {

    @InjectMocks
    private MemoAiEventListener memoAiEventListener;

    @Mock
    private AiRetryExecutor aiRetryExecutor;

    @Mock
    private MemoAnalysisService memoAnalysisService;

    @Test
    @DisplayName("AI 파싱 성공 시 실제 시도 횟수와 함께 분석을 완료한다")
    void handleAnalysisRequested_성공() {
        // given
        MemoAnalysisRequestedEvent event = new MemoAnalysisRequestedEvent(100L, 1L, "내일 2시 알고리즘 스터디");
        AiParserDto.Response mockResponse = new AiParserDto.Response(
                true,                  // isSchedule
                "알고리즘 스터디",                   // title
                "2026-08-21",              // scheduledAt
                "14:00",                   // scheduledTime
                "강남역 인근 스터디룸",               // location
                "알고리즘 문제 풀이 및 코드 리뷰",      // summaryInfo
                List.of("https://github.com/study") // actionLinks
        );

        given(aiRetryExecutor.execute(100L, 1L, "내일 2시 알고리즘 스터디"))
                .willReturn(new AiRetryExecutor.Result(mockResponse, 2));

        // when
        memoAiEventListener.handleAnalysisRequested(event);

        // then
        verify(memoAnalysisService).completeAnalysis(100L, mockResponse, 2);
    }

    @Test
    @DisplayName("재시도 소진 시 최종 실패 정보를 저장하고 예외를 격리한다")
    void handleAnalysisRequested_최종실패() {
        // given
        MemoAnalysisRequestedEvent event = new MemoAnalysisRequestedEvent(100L, 1L, "장애 테스트 메모");
        ResourceAccessException cause = new ResourceAccessException("FastAPI connection refused");
        given(aiRetryExecutor.execute(100L, 1L, "장애 테스트 메모"))
                .willThrow(new AiRetryExecutor.RetryExhaustedException(3, cause));

        // when
        memoAiEventListener.handleAnalysisRequested(event);

        // then
        verify(memoAnalysisService, never()).completeAnalysis(anyLong(), any(), anyInt());
        verify(memoAnalysisService).failAnalysis(100L, 3, "FastAPI connection refused");
    }
}
