package com.doquest.domain.memo.entity;

import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "memo_analyses")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class MemoAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "memo_analysis_id")
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "memo_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_memo_analyses_to_memos"))
    private Memo memo;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private MemoAnalysisStatus status;

    @Column(nullable = false)
    private boolean scheduleCandidate;

    @Column(length = 100)
    private String title;

    private LocalDate scheduledAt;

    private LocalTime scheduledTime;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String summaryInfo;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 500)
    private String lastError;

    public static MemoAnalysis pending(Memo memo) {
        MemoAnalysis analysis = new MemoAnalysis();
        analysis.memo = memo;
        analysis.status = MemoAnalysisStatus.PENDING;
        analysis.attemptCount = 0;
        return analysis;
    }

    public void complete(boolean scheduleCandidate, String title, LocalDate scheduledAt, LocalTime scheduledTime,
                         String location, String summaryInfo) {
        complete(scheduleCandidate, title, scheduledAt, scheduledTime, location, summaryInfo, 1);
    }

    public void complete(boolean scheduleCandidate, String title, LocalDate scheduledAt, LocalTime scheduledTime,
                         String location, String summaryInfo, int attemptCount) {
        this.scheduleCandidate = scheduleCandidate;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.scheduledTime = scheduledTime;
        this.location = location;
        this.summaryInfo = summaryInfo;
        this.attemptCount = attemptCount;
        this.lastError = null;
        this.status = MemoAnalysisStatus.SUCCEEDED;
    }

    public void complete(boolean scheduleCandidate, String title, LocalDate scheduledAt,
                         String location, String summaryInfo) {
        complete(scheduleCandidate, title, scheduledAt, null, location, summaryInfo);
    }

    public void fail() {
        fail(1, null);
    }

    public void fail(int attemptCount, String lastError) {
        this.status = MemoAnalysisStatus.FAILED;
        this.attemptCount = attemptCount;
        this.lastError = lastError;
    }

    public void restart() {
        this.status = MemoAnalysisStatus.PENDING;
        this.scheduleCandidate = false;
        this.title = null;
        this.scheduledAt = null;
        this.scheduledTime = null;
        this.location = null;
        this.summaryInfo = null;
        this.attemptCount = 0;
        this.lastError = null;
        this.memo.resetParsed();
    }

    public void confirm() {
        this.status = MemoAnalysisStatus.CONFIRMED;
    }
}
