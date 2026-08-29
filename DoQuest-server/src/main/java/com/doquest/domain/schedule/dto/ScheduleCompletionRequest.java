package com.doquest.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record ScheduleCompletionRequest(
        @NotNull(message = "완료 상태는 필수입니다.") Boolean completed
) {
}
