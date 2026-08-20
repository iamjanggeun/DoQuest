package com.doquest.domain.memo.event;

public record MemoCreatedEvent(
        Long memoId,
        Long memberId,
        String content
) {
}
