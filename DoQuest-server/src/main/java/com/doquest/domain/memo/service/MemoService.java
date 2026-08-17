package com.doquest.domain.memo.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemberRepository memberRepository;

    /**
     * 신규 메모 생성
     */
    @Transactional
    public Long createMemo(Long memberId, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Memo memo = Memo.createMemo(member, content);
        Memo savedMemo = memoRepository.save(memo);

        // TODO: Phase 4에서 비동기 AI 파이프라인(FastAPI + LangChain) 호출 이벤트 발행

        return savedMemo.getId();
    }

    /**
     * 회원의 최신 메모 목록 조회 (DTO로 변환하여 영속성 컨텍스트 의존 분리)
     */
    public List<MemoResponse> getMemosByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return memoRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(MemoResponse::from)
                .toList();
    }

    /**
     * 메모 내용 수정
     */
    @Transactional
    public void updateMemo(Long memberId, Long memoId, String newContent) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        validateMemoOwner(memberId, memo);
        memo.updateContent(newContent);
    }

    /**
     * 메모 삭제
     */
    @Transactional
    public void deleteMemo(Long memberId, Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        validateMemoOwner(memberId, memo);
        memoRepository.delete(memo);
    }

    /**
     * AI 파싱 완료 처리 (Phase 4 전용)
     */
    @Transactional
    public void completeParsing(Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        memo.markAsParsed();
    }

    // == 소유권 검증 (BOLA / IDOR 방어) == //
    private void validateMemoOwner(Long memberId, Memo memo) {
        if (!memo.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}