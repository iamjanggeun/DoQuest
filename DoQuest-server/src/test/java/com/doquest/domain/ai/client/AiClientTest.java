package com.doquest.domain.ai.client;

import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.global.config.AiClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(AiClient.class)
@Import(AiClientConfig.class)
@TestPropertySource(properties = {
        "ai.service.base-url=http://localhost:8000",
        "ai.service.connect-timeout-ms=1000",
        "ai.service.read-timeout-ms=2000"
})
@DisplayName("AiClient RestClient 슬라이스 테스트")
class AiClientTest {

    @Autowired
    private RestClient.Builder restClientBuilder;

    private AiClient aiClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // RestClient.Builder에 MockServer 바인딩
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        // MockServer가 바인딩된 Builder로 RestClient를 생성하여 AiClient에 주입
        RestClient restClient = restClientBuilder.baseUrl("http://localhost:8000").build();
        aiClient = new AiClient(restClient);
    }

    @Nested
    @DisplayName("parseMemo() 호출 시")
    class ParseMemoTest {

        @Test
        @DisplayName("[성공] FastAPI 정상 응답을 Java Record DTO로 완벽히 역직렬화한다")
        void parseMemo_Success() {
            String mockResponseJson = """
                    {
                        "is_schedule": true,
                        "title": "백준 골드 DP 문제 풀이",
                        "scheduled_at": "2026-08-18",
                        "summary_info": "동적 계획법 점화식 도출 및 풀이",
                        "action_links": [
                            "https://www.acmicpc.net"
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8000/api/v1/ai/parse-memo"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.member_id").value(1L))
                    .andExpect(jsonPath("$.content").value("내일 저녁 백준 DP 문제 풀기"))
                    .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

            AiParserDto.Response response = aiClient.parseMemo(1L, "내일 저녁 백준 DP 문제 풀기");

            assertThat(response).isNotNull();
            assertThat(response.isSchedule()).isTrue();
            assertThat(response.title()).isEqualTo("백준 골드 DP 문제 풀이");
            assertThat(response.scheduledAt()).isEqualTo("2026-08-18");
            assertThat(response.summaryInfo()).contains("동적 계획법");
            assertThat(response.actionLinks()).hasSize(1);
            assertThat(response.actionLinks().get(0)).isEqualTo("https://www.acmicpc.net");

            mockServer.verify();
        }

        @Test
        @DisplayName("[예외] AI 서버 500 에러 발생 시 HttpServerErrorException 예외를 전파한다")
        void parseMemo_ServerError() {
            mockServer.expect(requestTo("http://localhost:8000/api/v1/ai/parse-memo"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());

            assertThatThrownBy(() -> aiClient.parseMemo(1L, "내일 회의"))
                    .isInstanceOf(HttpServerErrorException.class);

            mockServer.verify();
        }
    }
}