package com.doquest.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.doquest.domain.schedule.entity.Schedule;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleResponse(
        Long scheduleId,
        Long memoId,
        String title,
        LocalDate scheduledAt,
        @JsonFormat(pattern = "HH:mm") LocalTime scheduledTime,
        String location,
        String summaryInfo,
        boolean isCompleted
) {
    public ScheduleResponse(Long scheduleId, Long memoId, String title, LocalDate scheduledAt,
                            String location, String summaryInfo, boolean isCompleted) {
        this(scheduleId, memoId, title, scheduledAt, null, location, summaryInfo, isCompleted);
    }

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getMemo() != null ? schedule.getMemo().getId() : null,
                schedule.getTitle(),
                schedule.getScheduledAt(),
                schedule.getScheduledTime(),
                schedule.getLocation(),
                schedule.getSummaryInfo(),
                schedule.isCompleted()
        );
    }
}
