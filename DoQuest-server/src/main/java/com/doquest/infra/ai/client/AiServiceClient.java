package com.doquest.infra.ai.client;

import com.doquest.infra.ai.dto.MemoAiParseRequest;
import com.doquest.infra.ai.dto.MemoAiParseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${ai.service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * FastAPI 서버로 비동기 LLM 파싱 요청 전송
     */
    public MemoAiParseResponse parseMemo(String content) {
        log.info("[AI 클라이언트] FastAPI 파싱 요청 전송: content='{}'", content);

        return restClient.post()
                .uri("/api/v1/ai/parse-memo")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MemoAiParseRequest(content))
                .retrieve()
                .body(MemoAiParseResponse.class);
    }
}