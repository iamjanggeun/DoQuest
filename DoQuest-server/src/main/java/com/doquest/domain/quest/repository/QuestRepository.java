package com.doquest.domain.quest.repository;

import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.entity.QuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    // 기존: findByMemberIdAndIsCompletedFalse
    // 변경: status 필드를 직접 조회하도록 변경
    List<Quest> findByMemberIdAndStatus(Long memberId, QuestStatus status);

    // 가장 최근 등록 퀘스트 조회 (필요 시)
    Optional<Quest> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
}
