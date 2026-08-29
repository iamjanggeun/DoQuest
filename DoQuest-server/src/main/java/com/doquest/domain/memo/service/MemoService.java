package com.doquest.domain.memo.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;
import com.doquest.domain.memo.repository.MemoAnalysisRepository;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemberRepository memberRepository;
    private final MemoAnalysisRepository memoAnalysisRepository;

    /**
     * 신규 메모 저장. AI 분석은 사용자의 명시적 요청으로 별도 시작한다.
     */
    @Transactional
    public Long createMemo(Long memberId, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Memo memo = Memo.createMemo(member, content);
        Memo savedMemo = memoRepository.save(memo);
        log.info("[메모 생성] memoId={}, memberId={}", savedMemo.getId(), memberId);
        return savedMemo.getId();
    }

    public List<MemoResponse> getMemosByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return memoRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(MemoResponse::from)
                .toList();
    }


    @Transactional
    public void updateMemo(Long memberId, Long memoId, String newContent) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        validateMemoOwner(memberId, memo);
        memoAnalysisRepository.findByMemoId(memoId).ifPresent(analysis -> {
            if (analysis.getStatus() == MemoAnalysisStatus.PENDING) {
                throw new BusinessException(ErrorCode.MEMO_ANALYSIS_IN_PROGRESS);
            }
            if (analysis.getStatus() != MemoAnalysisStatus.CONFIRMED) {
                memoAnalysisRepository.delete(analysis);
                memo.resetParsed();
            }
        });
        memo.updateContent(newContent);
    }

    @Transactional
    public void deleteMemo(Long memberId, Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        validateMemoOwner(memberId, memo);
        memoAnalysisRepository.findByMemoId(memoId).ifPresent(analysis -> {
            if (analysis.getStatus() == MemoAnalysisStatus.CONFIRMED) {
                throw new BusinessException(ErrorCode.MEMO_HAS_CONFIRMED_SCHEDULE);
            }
            memoAnalysisRepository.delete(analysis);
        });
        memoRepository.delete(memo);
    }

    @Transactional
    public void completeParsing(Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        memo.markAsParsed();
    }

    private void validateMemoOwner(Long memberId, Memo memo) {
        if (!memo.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
