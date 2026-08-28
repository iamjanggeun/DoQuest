package com.doquest.domain.memo.dto;

import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;

import java.time.LocalDate;

public record MemoAnalysisResponse(
        Long memoId,
        MemoAnalysisStatus status,
        boolean isSchedule,
        String title,
        LocalDate scheduledAt,
        String location,
        String summaryInfo
) {
    public static MemoAnalysisResponse from(MemoAnalysis analysis) {
        return new MemoAnalysisResponse(
                analysis.getMemo().getId(),
                analysis.getStatus(),
                analysis.isScheduleCandidate(),
                analysis.getTitle(),
                analysis.getScheduledAt(),
                analysis.getLocation(),
                analysis.getSummaryInfo()
        );
    }
}
