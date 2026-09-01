package com.doquest.domain.schedule.repository;

import com.doquest.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndMemberId(Long scheduleId, Long memberId);

    boolean existsByMemoId(Long memoId);

    // 특정 회원의 특정 기간(한 달 등) 일정 조회 (캘린더 렌더링용)
    List<Schedule> findByMemberIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            Long memberId, LocalDate startDate, LocalDate endDate
    );

    // D-3 이내 마감 임박 미완료 일정 조회 (퀘스트 큐레이팅용 최적화 쿼리)
    @Query("SELECT s FROM Schedule s " +
            "WHERE s.member.id = :memberId " +
            "AND s.isCompleted = false " +
            "AND s.scheduledAt BETWEEN :today AND :deadline " +
            "ORDER BY s.scheduledAt ASC")
    List<Schedule> findUpcomingSchedules(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today,
            @Param("deadline") LocalDate deadline
    );
}
