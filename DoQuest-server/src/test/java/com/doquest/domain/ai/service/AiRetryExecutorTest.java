package com.doquest.domain.ai.service;

import com.doquest.domain.ai.client.AiClient;
import com.doquest.domain.ai.dto.AiParserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiRetryExecutor 단위 테스트")
class AiRetryExecutorTest {

    @Mock
    private AiClient aiClient;

    private AiRetryExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AiRetryExecutor(aiClient, 3, 0, 2.0);
    }

    @Test
    @DisplayName("일시적 5xx 뒤 성공하면 두 번째 결과를 반환한다")
    void retryableFailureThenSuccess() {
        AiParserDto.Response response = successfulResponse();
        given(aiClient.parseMemo(1L, 2L, "memo"))
                .willThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
                .willReturn(response);

        AiRetryExecutor.Result result = executor.execute(1L, 2L, "memo");

        assertThat(result.response()).isEqualTo(response);
        assertThat(result.attemptCount()).isEqualTo(2);
        verify(aiClient, times(2)).parseMemo(1L, 2L, "memo");
    }

    @Test
    @DisplayName("네트워크 오류가 계속되면 세 번 뒤 최종 실패한다")
    void retryableFailureExhausted() {
        given(aiClient.parseMemo(1L, 2L, "memo"))
                .willThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> executor.execute(1L, 2L, "memo"))
                .isInstanceOf(AiRetryExecutor.RetryExhaustedException.class)
                .hasFieldOrPropertyWithValue("attemptCount", 3);
        verify(aiClient, times(3)).parseMemo(1L, 2L, "memo");
    }

    @Test
    @DisplayName("재시도 대상이 아닌 4xx는 한 번만 호출한다")
    void nonRetryableClientError() {
        given(aiClient.parseMemo(1L, 2L, "memo"))
                .willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> executor.execute(1L, 2L, "memo"))
                .isInstanceOf(AiRetryExecutor.RetryExhaustedException.class)
                .hasFieldOrPropertyWithValue("attemptCount", 1);
        verify(aiClient).parseMemo(1L, 2L, "memo");
    }

    private AiParserDto.Response successfulResponse() {
        return new AiParserDto.Response(
                true, "면접", "2026-09-02", "14:00", "강남", "준비", List.of()
        );
    }
}
