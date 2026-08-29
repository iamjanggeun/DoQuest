package com.doquest.domain.memo.event;

public record MemoAnalysisRequestedEvent(
        Long memoId,
        Long memberId,
        String content
) {
}
