package com.doquest.domain.schedule.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.schedule.dto.ScheduleCreateRequest;
import com.doquest.domain.schedule.dto.ScheduleResponse;
import com.doquest.domain.schedule.dto.ScheduleUpdateRequest;
import com.doquest.domain.schedule.entity.Schedule;
import com.doquest.domain.schedule.repository.ScheduleRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final MemoRepository memoRepository;
    private final Clock clock;

    @Transactional
    public ScheduleResponse createSchedule(Long memberId, ScheduleCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Memo memo = null;
        if (request.memoId() != null) {
            if (scheduleRepository.existsByMemoId(request.memoId())) {
                throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_EXISTS_FOR_MEMO);
            }
            memo = memoRepository.findByIdAndMemberId(request.memoId(), memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        }

        Schedule schedule = Schedule.createSchedule(
                member, memo, request.title(), request.scheduledAt(), request.scheduledTime(), request.location(), request.summaryInfo()
        );

        try {
            return ScheduleResponse.from(scheduleRepository.saveAndFlush(schedule));
        } catch (DataIntegrityViolationException e) {
            if (request.memoId() != null) {
                throw new BusinessException(ErrorCode.SCHEDULE_ALREADY_EXISTS_FOR_MEMO);
            }
            throw e;
        }
    }

    public List<ScheduleResponse> getMonthlySchedules(Long memberId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return scheduleRepository.findByMemberIdAndScheduledAtBetweenOrderByScheduledAtAsc(memberId, startDate, endDate)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public ScheduleResponse getSchedule(Long memberId, Long scheduleId) {
        return ScheduleResponse.from(findOwnedSchedule(memberId, scheduleId));
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long memberId, Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = findOwnedSchedule(memberId, scheduleId);
        schedule.update(request.title(), request.scheduledAt(), request.scheduledTime(), request.location(), request.summaryInfo());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public ScheduleResponse changeCompletion(Long memberId, Long scheduleId, boolean completed) {
        Schedule schedule = findOwnedSchedule(memberId, scheduleId);
        schedule.changeCompletion(completed);
        return ScheduleResponse.from(schedule);
    }

    public List<ScheduleResponse> getUpcomingCuration(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate dPlus3 = today.plusDays(3);

        return scheduleRepository.findUpcomingSchedules(memberId, today, dPlus3)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSchedule(Long memberId, Long scheduleId) {
        Schedule schedule = findOwnedSchedule(memberId, scheduleId);
        scheduleRepository.delete(schedule);
    }

    private Schedule findOwnedSchedule(Long memberId, Long scheduleId) {
        return scheduleRepository.findByIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }
}
