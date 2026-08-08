package com.doquest.domain.pet.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PetStage {

    EGG("알", 0),
    CRACKED("금간 알", 100),
    HATCHED("부화함", 500),
    BABY("아기", 1000);

    private final String description;
    private final int requiredExp;
}
