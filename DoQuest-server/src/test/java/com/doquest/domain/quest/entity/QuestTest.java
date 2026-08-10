package com.doquest.domain.quest.entity;

import com.doquest.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 26.08.10 퀘스트 어뷰징 코드 추가하면서 퀘스트 엔티티 전면 개편
// 새로운 테스트 코드
class QuestTest {

    @Test
    @DisplayName("퀘스트 완료 시 status가 COMPLETED로 변경되고 isCompleted()는 true를 반환한다.")
    public void 완료성공테스트() {
        // given
        Member member = Member.createMember("test@gmail.com", "password", "닉네임", null);
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀이", QuestCategory.STUDY, 20);

        // when
        quest.complete();

        // then
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(quest.isCompleted()).isTrue();
    }
}