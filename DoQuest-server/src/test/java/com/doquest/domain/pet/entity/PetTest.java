package com.doquest.domain.pet.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PetTest {

    @Test
    @DisplayName("경험치를 획득해도 다음 단계 기준 미달 시 레벨과 단계가 유지된다")
    public void addExp_maintainStage() throws Exception {
        // given
        Pet pet = Pet.createDefaultPet("키리코");

        // when
        pet.addExp(50); // BABY 단계 기준(100) 미달

        // then
        assertThat(pet.getExp()).isEqualTo(50);
        assertThat(pet.getLevel()).isEqualTo(1);
        assertThat(pet.getStage()).isEqualTo(PetStage.EGG);
    }

    @Test
    @DisplayName("경험치가 요구치를 충족하면 단계가 BABY로 상승하고 레벨이 1 증가한다")
    public void addExp_levelUpToBaby() throws Exception {
        // given
        Pet pet = Pet.createDefaultPet("레쿠쟈");

        // when
        pet.addExp(100); // BABY 단계 도달 (requiredExp: 100)

        // then
        assertThat(pet.getExp()).isEqualTo(100);
        assertThat(pet.getLevel()).isEqualTo(2);
        assertThat(pet.getStage()).isEqualTo(PetStage.BABY);
    }

    @Test
    @DisplayName("경험치가 대량으로 누적되어 SENIOR 단계까지 연속으로 레벨업한다")
    public void addExp_levelUpToSenior() throws Exception {
        // given
        Pet pet = Pet.createDefaultPet("로크");

        // when
        pet.addExp(1000); // SENIOR 단계 도달 (requiredExp: 1000)

        // then
        assertThat(pet.getExp()).isEqualTo(1000);
        assertThat(pet.getLevel()).isEqualTo(4); // EGG -> BABY(2) -> JUNIOR(3) -> SENIOR(4)
        assertThat(pet.getStage()).isEqualTo(PetStage.SENIOR);
    }
}