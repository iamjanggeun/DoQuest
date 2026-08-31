package com.doquest.domain.memo.dto;

import com.doquest.domain.memo.entity.Memo;

import java.time.Instant;

public record MemoResponse(
        Long id,
        String content,
        boolean isParsed,
        Instant createdAt
) {
    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getContent(),
                memo.isParsed(),
                memo.getCreatedAt()
        );
    }
}
