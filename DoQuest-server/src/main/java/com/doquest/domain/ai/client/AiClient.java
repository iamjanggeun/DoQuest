package com.doquest.domain.ai.client;

import com.doquest.domain.ai.dto.AiParserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient aiRestClient;

    public AiParserDto.Response parseMemo(Long memberId, String content) {
        log.info("[AI Client] 메모 파싱 요청 전송: memberId={}, content={}", memberId, content);

        return aiRestClient.post()
                .uri("/api/v1/ai/parse-memo")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AiParserDto.Request(memberId, content))
                .retrieve()
                .body(AiParserDto.Response.class);
    }
}