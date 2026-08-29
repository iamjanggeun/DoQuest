package com.doquest.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ScheduleUpdateRequest(
        @NotBlank(message = "일정 제목은 공백일 수 없습니다.") String title,
        @NotNull(message = "일정 날짜는 필수입니다.") LocalDate scheduledAt,
        String location,
        String summaryInfo
) {
}
