package com.doquest.domain.memo.dto;

import com.doquest.domain.memo.entity.Memo;

import java.time.LocalDateTime;

public record MemoResponse(
        Long id,
        String content,
        boolean isParsed,
        LocalDateTime createdAt
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