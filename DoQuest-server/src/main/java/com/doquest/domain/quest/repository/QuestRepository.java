package com.doquest.domain.quest.repository;

import com.doquest.domain.quest.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    List<Quest> findByMemberIdAndIsCompletedFalse(Long memberId);
}
