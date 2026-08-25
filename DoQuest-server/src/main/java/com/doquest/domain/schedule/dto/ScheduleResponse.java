package com.doquest.domain.schedule.dto;

import com.doquest.domain.schedule.entity.Schedule;
import java.time.LocalDate;

public record ScheduleResponse(
        Long scheduleId,
        Long memoId,
        String title,
        LocalDate scheduledAt,
        String location,
        String summaryInfo,
        boolean isCompleted
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getMemo() != null ? schedule.getMemo().getId() : null,
                schedule.getTitle(),
                schedule.getScheduledAt(),
                schedule.getLocation(),
                schedule.getSummaryInfo(),
                schedule.isCompleted()
        );
    }
}