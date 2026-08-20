package com.doquest.infra.ai.dto;

import java.util.List;

public record MemoAiParseResponse(
        boolean isSchedule,
        String title,
        String scheduledAt,
        String location,
        List<String> links,
        List<String> tags
) {
}
