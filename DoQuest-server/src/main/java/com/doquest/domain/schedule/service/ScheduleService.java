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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final MemoRepository memoRepository;

    @Transactional
    public ScheduleResponse createSchedule(Long memberId, ScheduleCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Memo memo = null;
        if (request.memoId() != null) {
            memo = memoRepository.findById(request.memoId()).orElse(null);
        }

        Schedule schedule = Schedule.createSchedule(
                member, memo, request.title(), request.scheduledAt(), request.location(), request.summaryInfo()
        );

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public List<ScheduleResponse> getMonthlySchedules(Long memberId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return scheduleRepository.findByMemberIdAndScheduledAtBetweenOrderByScheduledAtAsc(memberId, startDate, endDate)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public List<ScheduleResponse> getUpcomingCuration(Long memberId) {
        LocalDate today = LocalDate.now();
        LocalDate dPlus3 = today.plusDays(3);

        return scheduleRepository.findUpcomingSchedules(memberId, today, dPlus3)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSchedule(Long memberId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (!schedule.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        scheduleRepository.delete(schedule);
    }
}