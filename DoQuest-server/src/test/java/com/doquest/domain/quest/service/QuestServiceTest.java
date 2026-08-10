package com.doquest.domain.quest.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.pet.repository.PetRepository;
import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.entity.QuestCategory;
import com.doquest.domain.quest.entity.QuestStatus;
import com.doquest.domain.quest.repository.QuestRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    // 26.08.10 퀘스트 어뷰징 코드 추가하면서 퀘스트 엔티티 전면 개편
    // 새로운 테스트 코드
    @Test
    @DisplayName("정상적인 회원 ID와 입력값으로 퀘스트를 생성하면 퀘스트 ID가 반환된다.")
    void createQuest성공() {
        // given
        Long memberId = 1L;
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀기", QuestCategory.STUDY, 20);

        ReflectionTestUtils.setField(quest, "id", 100L); // 가상 PK 설정

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(questRepository.save(any(Quest.class))).willReturn(quest);

        // when
        Long savedId = questService.createQuest(memberId, "알고리즘 1문제 풀기", QuestCategory.STUDY);

        // then
        assertThat(savedId).isEqualTo(100L);
        verify(questRepository).save(any(Quest.class));
    }

    @Test
    @DisplayName("시작 후 30분이 지난 퀘스트는 정상적으로 완료 처리되고 펫 경험치가 지급된다.")
    void completeQuest_성공() {
        // given
        Long memberId = 1L;
        Long questId = 10L;

        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        ReflectionTestUtils.setField(member, "id", memberId);

        Quest quest = Quest.createQuest(member, "JPA 복습", QuestCategory.STUDY, 20);
        ReflectionTestUtils.setField(quest, "id", questId);

        // 💡 35분 전에 시작된 퀘스트로 타임스탬프 조작 (30분 쿨타임 가드레일 통과용)
        ReflectionTestUtils.setField(quest, "startedAt", LocalDateTime.now().minusMinutes(35));

        given(questRepository.findById(questId)).willReturn(Optional.of(quest));

        // when
        questService.completeQuest(memberId, questId);

        // then
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        verify(petRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("시작 후 30분이 지나지 않은 퀘스트 완료 시 QUEST_COMPLETE_TOO_FAST 예외가 발생한다.")
    void completeQuest_30분_미만_예외() {
        // given
        Long memberId = 1L;
        Long questId = 10L;

        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        ReflectionTestUtils.setField(member, "id", memberId);

        Quest quest = Quest.createQuest(member, "JPA 복습", QuestCategory.STUDY, 20);
        // 💡 방금(1분 전) 시작된 퀘스트
        ReflectionTestUtils.setField(quest, "startedAt", LocalDateTime.now().minusMinutes(1));

        given(questRepository.findById(questId)).willReturn(Optional.of(quest));

        // when & then
        assertThatThrownBy(() -> questService.completeQuest(memberId, questId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.QUEST_COMPLETE_TOO_FAST.getMessage());
    }

    @Test
    @DisplayName("다른 회원의 퀘스트를 완료 시도하면 INVALID_INPUT_VALUE 예외가 발생한다.")
    void completeQuest_타인_퀘스트_완료_예외() {
        // given
        Long requestMemberId = 1L;
        Long ownerMemberId = 2L;
        Long questId = 10L;

        Member owner = Member.createMember("owner@email.com", "pass", "주인", null);
        ReflectionTestUtils.setField(owner, "id", ownerMemberId);

        Quest quest = Quest.createQuest(owner, "타인의 퀘스트", QuestCategory.STUDY, 20);

        given(questRepository.findById(questId)).willReturn(Optional.of(quest));

        // when & then
        assertThatThrownBy(() -> questService.completeQuest(requestMemberId, questId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원의 미완료 퀘스트 조회 시 MEMBER_NOT_FOUND 예외가 발생한다.")
    void 미완료_조회시_회원이_없음() {
        // given
        given(memberRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> questService.findUncompletedQuests(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("존재하는 회원의 IN_PROGRESS 상태 퀘스트 목록을 성공적으로 조회한다.")
    void 미완료_퀘스트_목록_확인_테스트() {
        // given
        Long memberId = 1L;

        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Quest quest1 = Quest.createQuest(member, "JPA 복습", QuestCategory.STUDY, 20);
        Quest quest2 = Quest.createQuest(member, "알고리즘 풀기", QuestCategory.STUDY, 20);

        given(memberRepository.existsById(memberId)).willReturn(true);
        // 💡 Repository의 변경된 메서드 명칭(findByMemberIdAndStatus)에 동기화
        given(questRepository.findByMemberIdAndStatus(memberId, QuestStatus.IN_PROGRESS))
                .willReturn(List.of(quest1, quest2));

        // when
        List<Quest> result = questService.findUncompletedQuests(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(quest1, quest2);

        verify(memberRepository).existsById(memberId);
        verify(questRepository).findByMemberIdAndStatus(memberId, QuestStatus.IN_PROGRESS);
    }
}