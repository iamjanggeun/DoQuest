package com.doquest.domain.memo.repository;

import com.doquest.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    // 복합 인덱스(member_id, created_at)를 타는 최신순 조회 쿼리 메서드
    List<Memo> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<Memo> findByIdAndMemberId(Long memoId, Long memberId);
}
