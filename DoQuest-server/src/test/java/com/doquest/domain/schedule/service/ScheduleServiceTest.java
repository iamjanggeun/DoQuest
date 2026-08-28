package com.doquest.domain.schedule.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.schedule.dto.ScheduleCreateRequest;
import com.doquest.domain.schedule.dto.ScheduleResponse;
import com.doquest.domain.schedule.entity.Schedule;
import com.doquest.domain.schedule.repository.ScheduleRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 단위 테스트")
class ScheduleServiceTest {

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemoRepository memoRepository;

    private Member createMember(Long id) {
        Member member = Member.createMember("test@test.com", "1234", "Tester", null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Nested
    @DisplayName("일정 생성(createSchedule) 검증")
    class CreateScheduleServiceTest {

        @Test
        @DisplayName("[성공] 메모 기반 일정 생성 요청 시 정상적으로 저장된다.")
        void createSchedule_WithMemo_Success() {
            // given
            Long memberId = 1L;
            Long memoId = 10L;
            Member member = createMember(memberId);
            Memo memo = Memo.createMemo(member, "메모 본문");
            ReflectionTestUtils.setField(memo, "id", memoId);

            ScheduleCreateRequest request = new ScheduleCreateRequest(
                    memoId, "기술 면접", LocalDate.of(2026, 8, 28), "선릉", "면접 준비"
            );

            Schedule savedSchedule = Schedule.createSchedule(
                    member, memo, request.title(), request.scheduledAt(), request.location(), request.summaryInfo()
            );
            ReflectionTestUtils.setField(savedSchedule, "id", 100L);

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memoRepository.findByIdAndMemberId(memoId, memberId)).willReturn(Optional.of(memo));
            given(scheduleRepository.save(any(Schedule.class))).willReturn(savedSchedule);

            // when
            ScheduleResponse response = scheduleService.createSchedule(memberId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.scheduleId()).isEqualTo(100L);
            assertThat(response.memoId()).isEqualTo(memoId);
            assertThat(response.title()).isEqualTo("기술 면접");
            verify(scheduleRepository).save(any(Schedule.class));
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다.")
        void createSchedule_MemberNotFound_ThrowsException() {
            // given
            Long memberId = 999L;
            ScheduleCreateRequest request = new ScheduleCreateRequest(
                    null, "운동", LocalDate.of(2026, 8, 28), null, null
            );
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> scheduleService.createSchedule(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("일정 조회 및 큐레이션 검증")
    class GetScheduleServiceTest {

        @Test
        @DisplayName("[성공] 월별 일정 조회 시 해당 월의 시작일과 종료일 범위로 조회한다.")
        void getMonthlySchedules_Success() {
            // given
            Long memberId = 1L;
            int year = 2026;
            int month = 8;
            LocalDate startDate = LocalDate.of(2026, 8, 1);
            LocalDate endDate = LocalDate.of(2026, 8, 31);

            Member member = createMember(memberId);
            Schedule schedule = Schedule.createSchedule(
                    member, null, "일정 1", LocalDate.of(2026, 8, 15), null, null
            );
            ReflectionTestUtils.setField(schedule, "id", 1L);

            given(scheduleRepository.findByMemberIdAndScheduledAtBetweenOrderByScheduledAtAsc(memberId, startDate, endDate))
                    .willReturn(List.of(schedule));

            // when
            List<ScheduleResponse> responses = scheduleService.getMonthlySchedules(memberId, year, month);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).title()).isEqualTo("일정 1");
        }

        @Test
        @DisplayName("[성공] D-3 마감 임박 일정 큐레이션 목록을 반환한다.")
        void getUpcomingCuration_Success() {
            // given
            Long memberId = 1L;
            Member member = createMember(memberId);
            Schedule schedule = Schedule.createSchedule(
                    member, null, "D-2 마감 과제", LocalDate.now().plusDays(2), null, null
            );
            ReflectionTestUtils.setField(schedule, "id", 1L);

            given(scheduleRepository.findUpcomingSchedules(any(), any(), any()))
                    .willReturn(List.of(schedule));

            // when
            List<ScheduleResponse> responses = scheduleService.getUpcomingCuration(memberId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).title()).isEqualTo("D-2 마감 과제");
        }
    }

    @Nested
    @DisplayName("일정 삭제 검증")
    class DeleteScheduleServiceTest {

        @Test
        @DisplayName("[예외] 본인의 일정이 아닌 타인의 일정을 삭제하려고 하면 예외가 발생한다.")
        void deleteSchedule_OtherMemberSchedule_ThrowsException() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Long scheduleId = 100L;

            Member otherMember = createMember(otherMemberId);
            Schedule schedule = Schedule.createSchedule(
                    otherMember, null, "타인 일정", LocalDate.of(2026, 8, 28), null, null
            );

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

            // when & then
            assertThatThrownBy(() -> scheduleService.deleteSchedule(memberId, scheduleId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
