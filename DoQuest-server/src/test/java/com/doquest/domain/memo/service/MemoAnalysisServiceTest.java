package com.doquest.domain.memo.service;

import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;
import com.doquest.domain.memo.event.MemoAnalysisRequestedEvent;
import com.doquest.domain.memo.repository.MemoAnalysisRepository;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.schedule.dto.ScheduleResponse;
import com.doquest.domain.schedule.service.ScheduleService;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemoAnalysisServiceTest {

    @InjectMocks
    private MemoAnalysisService memoAnalysisService;

    @Mock
    private MemoAnalysisRepository memoAnalysisRepository;

    @Mock
    private MemoRepository memoRepository;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void requestAnalysis_성공() {
        Long memberId = 1L;
        Long memoId = 10L;
        MemoAnalysis analysis = createPendingAnalysis(memberId, memoId);
        Memo memo = analysis.getMemo();
        given(memoRepository.findByIdAndMemberId(memoId, memberId)).willReturn(Optional.of(memo));
        given(memoAnalysisRepository.findByMemoId(memoId)).willReturn(Optional.empty());
        given(memoAnalysisRepository.save(org.mockito.ArgumentMatchers.any(MemoAnalysis.class)))
                .willReturn(analysis);

        var result = memoAnalysisService.requestAnalysis(memberId, memoId);

        assertThat(result.status()).isEqualTo(MemoAnalysisStatus.PENDING);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MemoAnalysisRequestedEvent.class));
    }

    @Test
    void completeAnalysis_성공() {
        MemoAnalysis analysis = createPendingAnalysis(1L, 10L);
        AiParserDto.Response response = new AiParserDto.Response(
                true, "기술 면접", "2026-08-30", "14:30", "선릉",
                "면접 준비", List.of()
        );
        given(memoAnalysisRepository.findByMemoId(10L)).willReturn(Optional.of(analysis));

        memoAnalysisService.completeAnalysis(10L, response);

        assertThat(analysis.getStatus()).isEqualTo(MemoAnalysisStatus.SUCCEEDED);
        assertThat(analysis.getScheduledAt()).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(analysis.getScheduledTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(analysis.getMemo().isParsed()).isTrue();
        assertThat(analysis.getAttemptCount()).isEqualTo(1);
        assertThat(analysis.getLastError()).isNull();
    }

    @Test
    void failAnalysis_최종시도횟수와오류저장() {
        MemoAnalysis analysis = createPendingAnalysis(1L, 10L);
        given(memoAnalysisRepository.findByMemoId(10L)).willReturn(Optional.of(analysis));

        memoAnalysisService.failAnalysis(10L, 3, "connection refused\nstack detail");

        assertThat(analysis.getStatus()).isEqualTo(MemoAnalysisStatus.FAILED);
        assertThat(analysis.getAttemptCount()).isEqualTo(3);
        assertThat(analysis.getLastError()).isEqualTo("connection refused stack detail");
    }

    @Test
    void requestAnalysis_FAILED분석은초기화후재시작() {
        Long memberId = 1L;
        Long memoId = 10L;
        MemoAnalysis analysis = createPendingAnalysis(memberId, memoId);
        analysis.fail(3, "connection refused");
        Memo memo = analysis.getMemo();
        given(memoRepository.findByIdAndMemberId(memoId, memberId)).willReturn(Optional.of(memo));
        given(memoAnalysisRepository.findByMemoId(memoId)).willReturn(Optional.of(analysis));

        var result = memoAnalysisService.requestAnalysis(memberId, memoId);

        assertThat(result.status()).isEqualTo(MemoAnalysisStatus.PENDING);
        assertThat(analysis.getAttemptCount()).isZero();
        assertThat(analysis.getLastError()).isNull();
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(MemoAnalysisRequestedEvent.class));
    }

    @Test
    void completeAnalysis_잘못된시간형식_예외() {
        MemoAnalysis analysis = createPendingAnalysis(1L, 10L);
        AiParserDto.Response response = new AiParserDto.Response(
                true, "기술 면접", "2026-08-30", "오후 두 시", "선릉",
                "면접 준비", List.of()
        );
        given(memoAnalysisRepository.findByMemoId(10L)).willReturn(Optional.of(analysis));

        assertThatThrownBy(() -> memoAnalysisService.completeAnalysis(10L, response))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SCHEDULE_TIME);
    }

    @Test
    void confirmSchedule_성공() {
        MemoAnalysis analysis = createPendingAnalysis(1L, 10L);
        analysis.complete(true, "기술 면접", LocalDate.of(2026, 8, 30), "선릉", "면접 준비");
        ScheduleResponse saved = new ScheduleResponse(
                100L, 10L, "기술 면접", LocalDate.of(2026, 8, 30),
                "선릉", "면접 준비", false
        );
        given(memoAnalysisRepository.findByMemoIdAndMemoMemberId(10L, 1L))
                .willReturn(Optional.of(analysis));
        given(scheduleService.createSchedule(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willReturn(saved);

        ScheduleResponse result = memoAnalysisService.confirmSchedule(1L, 10L);

        assertThat(result.scheduleId()).isEqualTo(100L);
        assertThat(analysis.getStatus()).isEqualTo(MemoAnalysisStatus.CONFIRMED);

        ArgumentCaptor<com.doquest.domain.schedule.dto.ScheduleCreateRequest> captor =
                ArgumentCaptor.forClass(com.doquest.domain.schedule.dto.ScheduleCreateRequest.class);
        verify(scheduleService).createSchedule(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertThat(captor.getValue().memoId()).isEqualTo(10L);
    }

    @Test
    void confirmSchedule_중복확정_예외() {
        MemoAnalysis analysis = createPendingAnalysis(1L, 10L);
        analysis.complete(true, "기술 면접", LocalDate.of(2026, 8, 30), null, null);
        analysis.confirm();
        given(memoAnalysisRepository.findByMemoIdAndMemoMemberId(10L, 1L))
                .willReturn(Optional.of(analysis));

        assertThatThrownBy(() -> memoAnalysisService.confirmSchedule(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMO_ANALYSIS_ALREADY_CONFIRMED);
    }

    private MemoAnalysis createPendingAnalysis(Long memberId, Long memoId) {
        Member member = Member.createMember("test@example.com", "password", "tester", null);
        ReflectionTestUtils.setField(member, "id", memberId);
        Memo memo = Memo.createMemo(member, "다음 주 기술 면접");
        ReflectionTestUtils.setField(memo, "id", memoId);
        return MemoAnalysis.pending(memo);
    }
}
