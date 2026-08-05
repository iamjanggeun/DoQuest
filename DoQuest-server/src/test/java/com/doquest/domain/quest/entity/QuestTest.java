package com.doquest.domain.quest.entity;

import com.doquest.domain.member.entity.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;


class QuestTest {

    @Test
    @DisplayName("퀘스트 완료 시 isCompleted = true가 된다.")
    public void 완료성공테스트() throws Exception {
        //given
        Member member = Member.createMember("test@gmail.com", "password", "닉네임", null);
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀이", QuestCategory.STUDY, 50);
        //when
        quest.complete();
        //then
        assertThat(quest.isCompleted()).isEqualTo(true);
    }

    @Test
    @DisplayName("이미 완료된 퀘스트를 다시 완료 시도하면 예외가 발생한다.")
    public void 완료재실행예외발생() throws Exception {
        //given
        Member member = Member.createMember("test@gmail.com", "password", "닉네임", null);
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀이", QuestCategory.STUDY, 50);
        //when
        quest.complete();
        //then
        assertThatThrownBy(quest::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 퀘스트입니다.");;
    }
}