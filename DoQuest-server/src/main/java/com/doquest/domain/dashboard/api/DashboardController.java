package com.doquest.domain.dashboard.api;

import com.doquest.domain.dashboard.dto.DashboardResponse;
import com.doquest.domain.pet.entity.Pet;
import com.doquest.domain.pet.service.PetService; // 추가할 서비스
import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final QuestService questService;
    private final PetService petService;

    /**
     * 1번 화면: 펫 상태 및 당일 미완료 퀘스트 리스트 통합 조회
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal Long memberId
    ) {
        // 도메인 서비스에서 각각 데이터 조회
        Pet myPet = petService.getPetByMemberId(memberId);
        List<Quest> uncompletedQuests = questService.findUncompletedQuests(memberId);

        // 2. DTO로 조립하여 반환
        return ResponseEntity.ok(DashboardResponse.of(myPet, uncompletedQuests));
    }
}