package com.doquest.domain.quest.dto;

import com.doquest.domain.quest.entity.QuestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestCreateRequest(

        @NotBlank(message = "퀘스트 제목은 필수입니다.")
        String title,

        @NotNull(message = "카테고리를 지정해주세요.")
        QuestCategory category
) {
}
