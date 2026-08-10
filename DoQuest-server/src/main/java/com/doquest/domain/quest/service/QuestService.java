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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestService {

    private final QuestRepository questRepository;
    private final PetRepository petRepository;
    private final MemberRepository memberRepository;

    private static final int DEFAULT_REWARD_EXP = 20;
    private static final long COMPLETE_COOLDOWN_MINUTES = 30L; // 퀘스트 최소 수행 시간 (30분)

    /*
     * 신규 퀘스트 생성
     */
    @Transactional
    public Long createQuest(Long memberId, String title, QuestCategory category) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // TODO: Phase 4에서 LangChain / Vector DB 연동하여 '시맨틱 중복 퀘스트' 검증 로직이 들어갈 자리

        Quest quest = Quest.createQuest(member, title, category, DEFAULT_REWARD_EXP);
        Quest savedQuest = questRepository.save(quest);

        return savedQuest.getId();
    }

    /*
     * 퀘스트 완료 처리 및 펫 경험치 지급
     */
    @Transactional
    public void completeQuest(Long memberId, Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUEST_NOT_FOUND));

        // 퀘스트 소유권 검증 (다른 유저의 퀘스트 완료 요청 차단)
        if (!quest.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // 혹은 HANDLE_ACCESS_DENIED
        }

        // 이미 완료된 퀘스트 방지
        if (quest.getStatus() == QuestStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.QUEST_ALREADY_COMPLETED);
        }

        // startedAt 기준 30분 경과 여부 검증
        validateCompleteCooldown(quest);

        // 상태 변경
        quest.complete();

        // 펫 경험치 지급
        petRepository.findByMemberId(quest.getMember().getId())
                .ifPresent(pet -> pet.addExp(quest.getRewardExp()));
    }

    /*
     * 회원의 미완료 퀘스트 목록 조회
     */
    public List<Quest> findUncompletedQuests(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        // IN_PROGRESS 상태인 퀘스트 목록 조회
        return questRepository.findByMemberIdAndStatus(memberId, QuestStatus.IN_PROGRESS);
    }

    // == Validation Methods == //
    private void validateCompleteCooldown(Quest quest) {
        if (quest.getStartedAt() == null) {
            throw new BusinessException(ErrorCode.QUEST_NOT_IN_PROGRESS);
        }

        long minutes = Duration.between(quest.getStartedAt(), LocalDateTime.now()).toMinutes();
        if (minutes < COMPLETE_COOLDOWN_MINUTES) {
            throw new BusinessException(ErrorCode.QUEST_COMPLETE_TOO_FAST);
        }
    }
}