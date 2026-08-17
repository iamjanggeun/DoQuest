package com.doquest.domain.quest.api;

import com.doquest.domain.quest.dto.QuestCreateRequest;
import com.doquest.domain.quest.service.QuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    /**
     * 퀘스트 생성 (엔터 시 호출)
     */
    @PostMapping
    public ResponseEntity<Void> createQuest(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody QuestCreateRequest request) { // Valid로 검증 동작

        questService.createQuest(memberId, request.title(), request.category());

        // 생성 완료 후 201 Created 응답 (데이터는 대시보드 리로드 시 조회)
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}