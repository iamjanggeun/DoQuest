package com.doquest.domain.memo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record MemoAnalysisResponse(
        Long memoId,
        MemoAnalysisStatus status,
        boolean isSchedule,
        String title,
        LocalDate scheduledAt,
        @JsonFormat(pattern = "HH:mm") LocalTime scheduledTime,
        String location,
        String summaryInfo
) {
    public MemoAnalysisResponse(Long memoId, MemoAnalysisStatus status, boolean isSchedule, String title,
                                LocalDate scheduledAt, String location, String summaryInfo) {
        this(memoId, status, isSchedule, title, scheduledAt, null, location, summaryInfo);
    }

    public static MemoAnalysisResponse from(MemoAnalysis analysis) {
        return new MemoAnalysisResponse(
                analysis.getMemo().getId(),
                analysis.getStatus(),
                analysis.isScheduleCandidate(),
                analysis.getTitle(),
                analysis.getScheduledAt(),
                analysis.getScheduledTime(),
                analysis.getLocation(),
                analysis.getSummaryInfo()
        );
    }
}
