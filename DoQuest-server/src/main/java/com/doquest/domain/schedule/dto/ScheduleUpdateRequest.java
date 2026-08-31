package com.doquest.domain.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleUpdateRequest(
        @NotBlank(message = "일정 제목은 공백일 수 없습니다.") String title,
        @NotNull(message = "일정 날짜는 필수입니다.") LocalDate scheduledAt,
        @JsonFormat(pattern = "HH:mm") LocalTime scheduledTime,
        String location,
        String summaryInfo
) {
    public ScheduleUpdateRequest(String title, LocalDate scheduledAt, String location, String summaryInfo) {
        this(title, scheduledAt, null, location, summaryInfo);
    }
}
