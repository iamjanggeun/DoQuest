package com.doquest.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AiParserDto {

    public record Request(
            @JsonProperty("memo_id") Long memoId,
            @JsonProperty("member_id") Long memberId,
            @JsonProperty("content") String content
    ) {}

    public record Response(
            @JsonProperty("is_schedule") boolean isSchedule,
            @JsonProperty("title") String title,
            @JsonProperty("scheduled_at") String scheduledAt,
            @JsonProperty("location") String location,       // 추가: 장소 정보
            @JsonProperty("summary_info") String summaryInfo,
            @JsonProperty("action_links") List<String> actionLinks
    ) {}
}