package com.doquest.domain.memo.service;

import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.entity.MemoAnalysisStatus;
import com.doquest.domain.memo.repository.MemoAnalysisRepository;
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

import java.time.LocalDate;
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
    private ScheduleService scheduleService;

    @Test
    void completeAnalysis_성공() {
        MemoAnalysis analysis = createPendingAnalysis(1L, 10L);
        AiParserDto.Response response = new AiParserDto.Response(
                true, "기술 면접", "2026-08-30", "선릉",
                "면접 준비", List.of()
        );
        given(memoAnalysisRepository.findByMemoId(10L)).willReturn(Optional.of(analysis));

        memoAnalysisService.completeAnalysis(10L, response);

        assertThat(analysis.getStatus()).isEqualTo(MemoAnalysisStatus.SUCCEEDED);
        assertThat(analysis.getScheduledAt()).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(analysis.getMemo().isParsed()).isTrue();
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
