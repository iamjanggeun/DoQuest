package com.doquest.domain.pet.entity;

import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Pet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "pet_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int exp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetStage stage;

    // == Factory Method == //
    public static Pet createDefaultPet(String name) {
        Pet pet = new Pet();
        pet.name = name;
        pet.level = 1;
        pet.exp = 0;
        pet.stage = PetStage.EGG;
        return pet;
    }

    // == Business Logic == //
    /*
    * 경험치 획득 및 레벨업, 단계 상승 로직
    */
    public void addExp(int amount) {
        this.exp += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        if(this.exp >= this.stage.getRequiredExp()) {
            updateStage();
        }
    }

    // Pet.java 내부 메서드 수정
    private void updateStage() {
        // 현재 단계에서 다음 단계로 올라갈 조건이 만족되는 동안 반복
        while (true) {
            if (this.stage == PetStage.EGG && this.exp >= PetStage.CRACKED.getRequiredExp()) {
                this.stage = PetStage.CRACKED;
                this.level++;
            } else if (this.stage == PetStage.CRACKED && this.exp >= PetStage.HATCHED.getRequiredExp()) {
                this.stage = PetStage.HATCHED;
                this.level++;
            } else if (this.stage == PetStage.HATCHED && this.exp >= PetStage.BABY.getRequiredExp()) {
                this.stage = PetStage.BABY;
                this.level++;
            } else {
                break;
            }
        }
    }
}
