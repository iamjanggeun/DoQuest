package com.doquest.domain.memo.repository;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    List<Memo> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
