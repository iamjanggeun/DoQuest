package com.doquest.domain.memo.repository;

import com.doquest.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    // 회원의 메모 목록 최신순 조회
    List<Memo> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}