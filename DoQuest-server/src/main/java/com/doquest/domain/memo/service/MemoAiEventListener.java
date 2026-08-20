package com.doquest.domain.memo.service;

import com.doquest.domain.ai.client.AiClient;
import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.memo.event.MemoCreatedEvent;
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

    private final AiClient aiClient;
    private final MemoService memoService;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMemoCreated(MemoCreatedEvent event) {
        log.info("[AI 비동기 파이프라인 트리거] thread={}, memoId={}",
                Thread.currentThread().getName(), event.memoId());

        try {
            // FastAPI AI 엔진 호출 (LangChain + Gemini 추론)
            AiParserDto.Response response = aiClient.parseMemo(event.memberId(), event.content());

            log.info("[AI 파싱 성공] memoId={}, isSchedule={}, title='{}', links={}",
                    event.memoId(), response.is_schedule(), response.title(), response.action_links().size());

            // 메모 파싱 완료 플래그 갱신 (별도 신규 트랜잭션)
            memoService.completeParsing(event.memoId());

            // TODO (다음 단계): response.is_schedule()이 true인 경우 Schedule(일정) 엔티티 자동 생성 및 저장

        } catch (Exception e) {
            // 외부 AI/네트워크 장애 발생 시 사용자 요청(메모 저장)에 영향을 주지 않도록 격리 로깅
            log.error("[AI 파이프라인 장애 격리] memoId={} 분석 실패: {}", event.memoId(), e.getMessage());
        }
    }
}