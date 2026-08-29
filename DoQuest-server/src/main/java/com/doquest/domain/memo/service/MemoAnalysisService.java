package com.doquest.domain.memo.service;

import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.memo.dto.MemoAnalysisResponse;
import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.event.MemoAnalysisRequestedEvent;
import com.doquest.domain.memo.repository.MemoAnalysisRepository;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.schedule.dto.ScheduleCreateRequest;
import com.doquest.domain.schedule.dto.ScheduleResponse;
import com.doquest.domain.schedule.service.ScheduleService;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoAnalysisService {

    private final MemoAnalysisRepository memoAnalysisRepository;
    private final MemoRepository memoRepository;
    private final ScheduleService scheduleService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MemoAnalysisResponse requestAnalysis(Long memberId, Long memoId) {
        Memo memo = memoRepository.findByIdAndMemberId(memoId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        MemoAnalysis analysis = memoAnalysisRepository.findByMemoId(memoId)
                .map(this::restartIfPossible)
                .orElseGet(() -> memoAnalysisRepository.save(MemoAnalysis.pending(memo)));

        eventPublisher.publishEvent(new MemoAnalysisRequestedEvent(memoId, memberId, memo.getContent()));
        return MemoAnalysisResponse.from(analysis);
    }

    public MemoAnalysisResponse getAnalysis(Long memberId, Long memoId) {
        return MemoAnalysisResponse.from(findOwnedAnalysis(memberId, memoId));
    }

    @Transactional
    public void completeAnalysis(Long memoId, AiParserDto.Response response) {
        MemoAnalysis analysis = findAnalysis(memoId);
        LocalDate scheduledAt = parseScheduledAt(response.scheduledAt());
        analysis.complete(response.isSchedule(), response.title(), scheduledAt,
                response.location(), response.summaryInfo());
        analysis.getMemo().markAsParsed();
    }

    @Transactional
    public void failAnalysis(Long memoId) {
        memoAnalysisRepository.findByMemoId(memoId).ifPresent(MemoAnalysis::fail);
    }

    @Transactional
    public ScheduleResponse confirmSchedule(Long memberId, Long memoId) {
        MemoAnalysis analysis = findOwnedAnalysis(memberId, memoId);

        if (analysis.getStatus() == MemoAnalysisStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.MEMO_ANALYSIS_ALREADY_CONFIRMED);
        }
        if (analysis.getStatus() != MemoAnalysisStatus.SUCCEEDED
                || !analysis.isScheduleCandidate()
                || analysis.getScheduledAt() == null) {
            throw new BusinessException(ErrorCode.MEMO_ANALYSIS_NOT_CONFIRMABLE);
        }

        ScheduleResponse schedule = scheduleService.createSchedule(
                memberId,
                new ScheduleCreateRequest(
                        memoId,
                        analysis.getTitle(),
                        analysis.getScheduledAt(),
                        analysis.getLocation(),
                        analysis.getSummaryInfo()
                )
        );
        analysis.confirm();
        return schedule;
    }

    private MemoAnalysis findOwnedAnalysis(Long memberId, Long memoId) {
        return memoAnalysisRepository.findByMemoIdAndMemoMemberId(memoId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMO_ANALYSIS_NOT_FOUND));
    }

    private MemoAnalysis findAnalysis(Long memoId) {
        return memoAnalysisRepository.findByMemoId(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMO_ANALYSIS_NOT_FOUND));
    }

    private MemoAnalysis restartIfPossible(MemoAnalysis analysis) {
        if (analysis.getStatus() == MemoAnalysisStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.MEMO_ANALYSIS_ALREADY_CONFIRMED);
        }
        if (analysis.getStatus() == MemoAnalysisStatus.PENDING) {
            throw new BusinessException(ErrorCode.MEMO_ANALYSIS_IN_PROGRESS);
        }
        analysis.restart();
        return analysis;
    }

    private LocalDate parseScheduledAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_DATE);
        }
    }
}
