package com.doquest.domain.quest.repository;

import com.doquest.domain.quest.entity.Quest;

import java.util.List;

public interface QuestRepository {
    List<Quest> findByMemberIdAndIsCompletedFalse(Long memberId);
}
