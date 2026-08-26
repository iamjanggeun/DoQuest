package com.doquest.domain.memo.service;

import com.doquest.domain.ai.client.AiClient;
import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.memo.event.MemoCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpServerErrorException;

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
    private AiClient aiClient;

    @Mock
    private MemoService memoService;

    @Test
    @DisplayName("AI 파싱 성공 시 MemoService.completeParsing()을 호출한다")
    void handleMemoCreated_성공() {
        // given
        MemoCreatedEvent event = new MemoCreatedEvent(100L, 1L, "내일 2시 알고리즘 스터디");
        AiParserDto.Response mockResponse = new AiParserDto.Response(
                true,                  // isSchedule
                "알고리즘 스터디",                   // title
                "2026-08-21 14:00",              // scheduledAt
                "강남역 인근 스터디룸",               // location
                "알고리즘 문제 풀이 및 코드 리뷰",      // summaryInfo
                List.of("https://github.com/study") // actionLinks
        );

        given(aiClient.parseMemo(100L, 1L, "내일 2시 알고리즘 스터디")).willReturn(mockResponse);

        // when
        memoAiEventListener.handleMemoCreated(event);

        // then
        verify(aiClient).parseMemo(100L, 1L, "내일 2시 알고리즘 스터디");
        verify(memoService).completeParsing(100L);
    }

    @Test
    @DisplayName("FastAPI 서버 500 장애 발생 시 예외를 격리하고 상위로 전파하지 않는다")
    void handleMemoCreated_AI서버장애_내결함성_격리() {
        // given
        MemoCreatedEvent event = new MemoCreatedEvent(100L, 1L, "장애 테스트 메모");
        given(aiClient.parseMemo(100L, 1L,"장애 테스트 메모"))
                .willThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500), "FastAPI Server Down"));

        // when
        memoAiEventListener.handleMemoCreated(event);

        // then
        verify(aiClient).parseMemo(100L, 1L ,"장애 테스트 메모");
        // 장애 시 파싱 완료 플래그 처리는 호출되지 않음
        verify(memoService, never()).completeParsing(anyLong());
    }
}