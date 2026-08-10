package com.doquest.domain.quest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestStatus {

    IN_PROGRESS("진행 중"),
    COMPLETED("완료됨");

    private final String description;
}
