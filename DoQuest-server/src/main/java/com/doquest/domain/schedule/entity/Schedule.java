package com.doquest.domain.schedule.entity;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "schedules",
        indexes = {
                // 캘린더 월별/일별 조회 및 D-3 마감 큐레이션 인덱스 최적화
                @Index(name = "idx_schedules_member_date", columnList = "member_id, scheduled_at")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_schedules_to_members"), nullable = false)
    private Member member;

    // 비정형 메모에서 파생된 경우 연결 (수동 생성 시 null 허용)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "memo_id", foreignKey = @ForeignKey(name = "fk_schedules_to_memos"))
    private Memo memo;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDate scheduledAt;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String summaryInfo;

    @Column(nullable = false)
    private boolean isCompleted;

    // == Factory Method (정적 팩토리 메서드) == //
    public static Schedule createSchedule(
            Member member,
            Memo memo,
            String title,
            LocalDate scheduledAt,
            String location,
            String summaryInfo
    ) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("일정 제목은 필수 입력값입니다.");
        }
        if (scheduledAt == null) {
            throw new IllegalArgumentException("일정 날짜는 필수 입력값입니다.");
        }

        Schedule schedule = new Schedule();
        schedule.member = member;
        schedule.memo = memo;
        schedule.title = title;
        schedule.scheduledAt = scheduledAt;
        schedule.location = location;
        schedule.summaryInfo = summaryInfo;
        schedule.isCompleted = false;
        return schedule;
    }

    // == 비즈니스 로직 == //
    public void update(String title, LocalDate scheduledAt, String location, String summaryInfo) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (scheduledAt != null) {
            this.scheduledAt = scheduledAt;
        }
        this.location = location;
        this.summaryInfo = summaryInfo;
    }

    public void toggleComplete() {
        this.isCompleted = !this.isCompleted;
    }
}