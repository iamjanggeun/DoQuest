package com.doquest.domain.pet.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PetStage {

    EGG("알", 0),
    BABY("아기", 100),
    JUNIOR("자라나는 중", 500),
    SENIOR("성체", 1000);

    private final String description;
    private final int requiredExp;
}
