package com.doquest.domain.schedule.controller;

import com.doquest.domain.schedule.dto.ScheduleCreateRequest;
import com.doquest.domain.schedule.dto.ScheduleResponse;
import com.doquest.domain.schedule.dto.ScheduleUpdateRequest;
import com.doquest.domain.schedule.dto.ScheduleCompletionRequest;
import com.doquest.domain.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 일정 수동/컨펌 등록
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ScheduleCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createSchedule(memberId, request));
    }

    // 캘린더 월별 조회 (예: /api/v1/schedules?year=2026&month=8)
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getMonthlySchedules(
            @AuthenticationPrincipal Long memberId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(scheduleService.getMonthlySchedules(memberId, year, month));
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId
    ) {
        return ResponseEntity.ok(scheduleService.getSchedule(memberId, scheduleId));
    }

    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(memberId, scheduleId, request));
    }

    @PatchMapping("/{scheduleId}/completion")
    public ResponseEntity<ScheduleResponse> changeCompletion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleCompletionRequest request
    ) {
        return ResponseEntity.ok(scheduleService.changeCompletion(memberId, scheduleId, request.completed()));
    }

    // D-3 마감 임박 큐레이팅 조회 (대시보드/퀘스트 화면용)
    @GetMapping("/curations")
    public ResponseEntity<List<ScheduleResponse>> getUpcomingCurations(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(scheduleService.getUpcomingCuration(memberId));
    }

    // 일정 삭제
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long scheduleId
    ) {
        scheduleService.deleteSchedule(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
