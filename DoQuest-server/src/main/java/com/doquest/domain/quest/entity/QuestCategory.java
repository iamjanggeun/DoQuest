package com.doquest.domain.quest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestCategory {

    STUDY("학습"),
    EXERCISE("운동"),
    HEALTH("건강"),
    LIFESTYLE("생활습관");

    private final String description;
}
