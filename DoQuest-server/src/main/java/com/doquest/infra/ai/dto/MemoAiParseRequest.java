package com.doquest.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MemoAiParseRequest(
        @JsonProperty("memo_id") Long memoId,
        @JsonProperty("member_id") Long memberId,
        @JsonProperty("content") String content
) {
}
