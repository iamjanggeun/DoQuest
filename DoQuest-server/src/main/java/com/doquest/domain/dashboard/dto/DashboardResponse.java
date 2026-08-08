package com.doquest.domain.dashboard.dto;

import com.doquest.domain.pet.entity.Pet;
import com.doquest.domain.quest.entity.Quest;

import java.util.List;

public record DashboardResponse(
        PetInfo pet,
        List<QuestInfo> quests
) {
    // 엔티티 DTO로 변환
    public static DashboardResponse of(Pet pet, List<Quest> quests) {
        return new DashboardResponse(
                PetInfo.from(pet),
                quests.stream().map(QuestInfo::from).toList()
        );
    }

    // 내부 레코드 : 펫 정보
    public record PetInfo(Long id, String name, int level, int exp, String stage) {
        public static PetInfo from(Pet pet) {
            return new PetInfo(pet.getId(), pet.getName(), pet.getLevel(), pet.getExp(), pet.getStage().getDescription());
        }
    }

    // 내부 레코드 : 퀘스트 정보
    public record QuestInfo(Long id, String title, String category, int rewardExp) {
        public static QuestInfo from(Quest quest) {
            return new QuestInfo(quest.getId(), quest.getTitle(), quest.getCategory().getDescription(), quest.getRewardExp());
        }
    }
}
