package com.doquest.domain.quest.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.pet.repository.PetRepository;
import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.entity.QuestCategory;
import com.doquest.domain.quest.repository.QuestRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @InjectMocks
    private QuestService questService;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PetRepository petRepository;

    @Test
    @DisplayName("정상적인 회원 ID와 입력값으로 퀘스트를 생성하면 퀘스트 ID가 반환된다")
    public void createQuest성공() {
        // given
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀기", QuestCategory.STUDY, 50);

        // Reflection이나 별도 설정을 통해 저장된 ID를 가상으로 부여 (Mockito matchers 활용)
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(questRepository.save(any(Quest.class))).willReturn(quest);
        // when
        Long savedId = questService.createQuest(1L, "알고리즘 1문제 풀기", QuestCategory.STUDY);

        // then
        assertThat(quest.getTitle()).isEqualTo("알고리즘 1문제 풀기");
        assertThat(quest.getCategory()).isEqualTo(QuestCategory.STUDY);
        verify(questRepository).save(any(Quest.class)); // save 메서드가 실제 호출되었는지 행위 검증
    }

    @Test
    @DisplayName("존재하지 않는 퀘스트 완료 시도 시 Exception이 발생한다.")
    public void 없는_퀘스트_완료시도_테스트() throws Exception {
        //given
        given(questRepository.findById(99L)).willReturn(Optional.empty());
        //when & then
        assertThatThrownBy(() -> questService.completeQuest(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.QUEST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원의 미완료 퀘스트 목록을 조회할 때 회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 발생한다")
    void 미완료_조회시_회원이_없음() {
        // given
        given(memberRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> questService.findUncompletedQuests(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("존재하는 회원의 미완료 퀘스트 목록을 성공적으로 조회한다")
    void 미완료_퀘스트_목록_확인_테스트() {
        // given
        Long memberId = 1L;

        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Quest uncompletedQuest1 = Quest.createQuest(member, "JPA 복습", QuestCategory.STUDY, 50);
        Quest uncompletedQuest2 = Quest.createQuest(member, "알고리즘 풀기", QuestCategory.STUDY, 50);

        given(memberRepository.existsById(memberId)).willReturn(true);
        given(questRepository.findByMemberIdAndIsCompletedFalse(memberId))
                .willReturn(List.of(uncompletedQuest1, uncompletedQuest2));

        // when
        List<Quest> result = questService.findUncompletedQuests(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(uncompletedQuest1, uncompletedQuest2);

        // 비즈니스 로직에 맞게 정확한 Repository 메서드를 호출했는지 행위 검증
        verify(memberRepository).existsById(memberId);
        verify(questRepository).findByMemberIdAndIsCompletedFalse(memberId);
    }
}