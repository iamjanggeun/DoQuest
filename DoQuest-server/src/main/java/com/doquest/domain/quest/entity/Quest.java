package com.doquest.domain.quest.entity;

import com.doquest.domain.member.entity.Member;
import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "quests",
        indexes = {
                // is_completed 대신 status를 조건절로 사용하는 복합 인덱스로 재설계
                @Index(name = "idx_quests_member_status", columnList = "member_id, status")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Quest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "quest_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_quests_to_members"), nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestStatus status; // IN_PROGRESS, COMPLETED 추가 - 26.08.10

    @Column(nullable = false)
    private int rewardExp;

    @Column(nullable = false)
    private LocalDateTime startedAt; // 30분 타이머 가드레일 검증용 시각

    // == Factory Method == //
    public static Quest createQuest(Member member, String title, QuestCategory category, int rewardExp) {
        Quest quest = new Quest();
        quest.member = member;
        quest.title = title;
        quest.category = category;
        quest.rewardExp = rewardExp;
        quest.status = QuestStatus.IN_PROGRESS; // 생성 시 자동 진행 중 상태
        quest.startedAt = LocalDateTime.now();  // 생성 직후 30분 타이머 시작
        return quest;
    }

    // == 비즈니스 로직 == //

    /**
     * 퀘스트 완료 처리
     */
    public void complete() {
        this.status = QuestStatus.COMPLETED;
    }

    // == 상태 조회용 도메인 메서드 (Convenience Method) == //

    /**
     * 기존 isCompleted 필드를 대체하여 기존 서비스/조회 로직의 하위 호환성을 보장
     */
    public boolean isCompleted() {
        return this.status == QuestStatus.COMPLETED;
    }
}