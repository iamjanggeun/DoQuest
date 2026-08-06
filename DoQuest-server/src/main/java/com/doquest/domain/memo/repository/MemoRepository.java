package com.doquest.domain.memo.repository;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.entity.Memo;

import java.util.List;

public interface MemoRepository {

    List<Memo> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
