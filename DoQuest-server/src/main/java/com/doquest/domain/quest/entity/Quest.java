package com.doquest.domain.quest.entity;

import com.doquest.domain.member.entity.Member;
import com.doquest.global.common.BaseTimeEntity;
import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "quests",
        indexes = {
                @Index(name = "idx_quests_member_completed", columnList = "member_id, is_completed")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quest_id")
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_quests_to_members"), nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestCategory category;

    @Column(nullable = false)
    private int rewardExp;

    @Column(nullable = false)
    private boolean isCompleted;

    // == Factory Method == //
    public static Quest createQuest(Member member, String title, QuestCategory category, int rewardExp) {
        Quest quest = new Quest();
        quest.member = member;
        quest.title = title;
        quest.category = category;
        quest.rewardExp = rewardExp;
        return quest;
    }

    // == 비즈니스 로직 == //
    /**
     * 퀘스트 완료 처리
     */
    public void complete() {
        if (this.isCompleted) {
            throw new IllegalStateException("이미 완료된 퀘스트입니다.");
        }
        this.isCompleted = true;
    }
}
