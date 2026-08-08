package com.doquest.domain.quest.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.pet.repository.PetRepository;
import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.entity.QuestCategory;
import com.doquest.domain.quest.repository.QuestRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestService {

    private final QuestRepository questRepository;
    private final PetRepository petRepository;
    private final MemberRepository memberRepository;

    private static final int DEFAULT_REWARD_EXP = 20;

    /*
    * 신규 퀘스트 생성
    * */
    @Transactional
    public Long createQuest(Long memberId, String title, QuestCategory category) {
        // 엔티티 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // TODO: Phase 4에서 LangChain / Vector DB 연동하여 '시맨틱 중복 퀘스트' 검증 로직이 들어갈 자리 (semanticDuplicateQuest)

        // 퀘스트 생성
        Quest quest = Quest.createQuest(member, title, category, DEFAULT_REWARD_EXP);
        // 퀘스트 저장
        Quest savedQuest = questRepository.save(quest);

        return savedQuest.getId();
    }

    /*
    * 퀘스트 완료 처리 및 펫 경험치 지급
    * */
    @Transactional
    public void completeQuest(Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(()->new BusinessException(ErrorCode.QUEST_NOT_FOUND));
        quest.complete();
        petRepository.findByMemberId(quest.getMember().getId())
                .ifPresent(pet -> pet.addExp(quest.getRewardExp()));
    }

    /*
     * 회원의 미완료 퀘스트 목록 조회
     **/
    public List<Quest> findUncompletedQuests(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return questRepository.findByMemberIdAndIsCompletedFalse(memberId);
    }
}
