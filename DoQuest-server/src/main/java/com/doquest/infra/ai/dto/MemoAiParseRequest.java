package com.doquest.infra.ai.dto;

public record MemoAiParseRequest(
        Long memoId,
        Long memberId,
        String content
) {
}
