package com.doquest.domain.memo.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.event.MemoCreatedEvent;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행기 주입

    /**
     * 신규 메모 생성 및 비동기 AI 파싱 이벤트 트리거 추가
     */
    @Transactional
    public Long createMemo(Long memberId, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Memo memo = Memo.createMemo(member, content);
        Memo savedMemo = memoRepository.save(memo);

        // 비동기 AI 파싱 도메인 이벤트 발행 (트랜잭션 Commit 완료 후 리스너 동작)
        eventPublisher.publishEvent(new MemoCreatedEvent(
                savedMemo.getId(),
                member.getId(),
                savedMemo.getContent()
        ));

        log.info("[메모 생성] memoId={}, memberId={}, AI 파싱 이벤트 발행 완료", savedMemo.getId(), memberId);
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
        memo.updateContent(newContent);
    }

    @Transactional
    public void deleteMemo(Long memberId, Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        validateMemoOwner(memberId, memo);
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