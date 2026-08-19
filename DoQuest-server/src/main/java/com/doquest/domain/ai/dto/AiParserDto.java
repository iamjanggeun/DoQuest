package com.doquest.domain.ai.dto;

import java.util.List;

public class AiParserDto {

    public record Request(Long member_id, String content) {}

    public record Response(
            boolean is_schedule,
            String title,
            String target_date,
            String summary_info,
            List<String> action_links
    ) {}
}