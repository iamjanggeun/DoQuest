package com.doquest.domain.memo.repository;

import com.doquest.domain.memo.entity.MemoAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemoAnalysisRepository extends JpaRepository<MemoAnalysis, Long> {
    Optional<MemoAnalysis> findByMemoId(Long memoId);
    Optional<MemoAnalysis> findByMemoIdAndMemoMemberId(Long memoId, Long memberId);

}
