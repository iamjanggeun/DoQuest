package com.doquest.domain.memo.service;

import com.doquest.domain.ai.service.AiRetryExecutor;
import com.doquest.domain.memo.event.MemoAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoAiEventListener {

    private final AiRetryExecutor aiRetryExecutor;
    private final MemoAnalysisService memoAnalysisService;

    /**
     * 사용자의 AI 분석 요청 트랜잭션이 커밋된 직후 비동기로 실행
     * - @Async("aiTaskExecutor"): 전용 스레드 풀에서 비동기 격리 실행
     * - @TransactionalEventListener(phase = AFTER_COMMIT): DB 커밋 성공 시에만 AI 파이프라인 트리거
     * - @Transactional(propagation = REQUIRES_NEW): AI 결과 반영을 위한 독립 트랜잭션 수명주기 보장
     */
    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAnalysisRequested(MemoAnalysisRequestedEvent event) {
        log.info("[AI 비동기 파이프라인 트리거] thread={}, memoId={}, memberId={}",
                Thread.currentThread().getName(), event.memoId(), event.memberId());

        try {
            // FastAPI AI 엔진 호출 (LangChain + LLM 추론)
            AiRetryExecutor.Result result = aiRetryExecutor.execute(
                    event.memoId(), event.memberId(), event.content());
            var response = result.response();

            log.info("[AI 파싱 성공] memoId={}, isSchedule={}, title='{}', links={}",
                    event.memoId(), response.isSchedule(), response.title(),
                    response.actionLinks() != null ? response.actionLinks().size() : 0);

            // 분석 결과와 메모 파싱 완료 상태를 독립 트랜잭션에 함께 반영
            memoAnalysisService.completeAnalysis(event.memoId(), response, result.attemptCount());

            // 후속 파이프라인 확장 포인트: 일정 감지 시 Schedule 도메인 연동
            // MemoAiEventListener.java 내부
            if (response.isSchedule()) {
                log.info("[일정 자동 등록 대상 감지] memoId={}, scheduledAt={}, location={}",
                        event.memoId(), response.scheduledAt(), response.location());
                // TODO: scheduleService.createScheduleFromAi(event.memberId(), event.memoId(), response);
            }

        } catch (AiRetryExecutor.RetryExhaustedException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            memoAnalysisService.failAnalysis(event.memoId(), e.getAttemptCount(), cause.getMessage());
            // 외부 AI 통신 장애/타임아웃이 분석 요청 응답에 전파되지 않도록 장애 격리 로깅
            log.error("[AI 파이프라인 장애 격리] memoId={} attempts={} 분석 실패 - Cause: {}",
                    event.memoId(), e.getAttemptCount(), cause.getMessage(), cause);
        } catch (Exception e) {
            memoAnalysisService.failAnalysis(event.memoId(), 1, e.getMessage());
            log.error("[AI 응답 처리 실패] memoId={} 분석 실패 - Cause: {}", event.memoId(), e.getMessage(), e);
        }
    }
}
