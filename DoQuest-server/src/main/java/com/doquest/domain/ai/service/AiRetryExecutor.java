package com.doquest.domain.ai.service;

import com.doquest.domain.ai.client.AiClient;
import com.doquest.domain.ai.dto.AiParserDto;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AiRetryExecutor {

    private final AiClient aiClient;
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final double backoffMultiplier;

    public AiRetryExecutor(
            AiClient aiClient,
            @Value("${ai.retry.max-attempts:3}") int maxAttempts,
            @Value("${ai.retry.initial-backoff-ms:500}") long initialBackoffMs,
            @Value("${ai.retry.backoff-multiplier:2.0}") double backoffMultiplier
    ) {
        this.aiClient = aiClient;
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    public Result execute(Long memoId, Long memberId, String content) {
        long backoffMs = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return new Result(aiClient.parseMemo(memoId, memberId, content), attempt);
            } catch (Exception e) {
                if (attempt == maxAttempts || !isRetryable(e)) {
                    throw new RetryExhaustedException(attempt, e);
                }
                sleep(backoffMs, attempt, e);
                backoffMs = Math.max(backoffMs, Math.round(backoffMs * backoffMultiplier));
            }
        }

        throw new IllegalStateException("AI retry loop completed without a result");
    }

    private boolean isRetryable(Exception exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        if (exception instanceof RestClientResponseException responseException) {
            HttpStatus status = HttpStatus.resolve(responseException.getStatusCode().value());
            return responseException.getStatusCode().is5xxServerError()
                    || status == HttpStatus.TOO_MANY_REQUESTS;
        }
        return false;
    }

    private void sleep(long backoffMs, int attempt, Exception lastFailure) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RetryExhaustedException(attempt, lastFailure);
        }
    }

    public record Result(AiParserDto.Response response, int attemptCount) {
    }

    @Getter
    public static class RetryExhaustedException extends RuntimeException {

        private final int attemptCount;

        public RetryExhaustedException(int attemptCount, Throwable cause) {
            super(cause);
            this.attemptCount = attemptCount;
        }
    }
}
