package com.doquest.domain.dashboard.api;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.pet.entity.Pet;
import com.doquest.domain.pet.service.PetService;
import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.entity.QuestCategory;
import com.doquest.domain.quest.service.QuestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestService questService;

    @MockitoBean
    private PetService petService;

    @Test
    @DisplayName("대시보드 조회 시 펫 정보와 미완료 퀘스트(status=IN_PROGRESS 포함) 목록이 통합 반환된다")
    void getDashboard_성공() throws Exception {
        // given
        Long memberId = 1L;
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Pet pet = Pet.createDefaultPet("알코믹");
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀기", QuestCategory.STUDY, 20);

        given(petService.getPetByMemberId(memberId)).willReturn(pet);
        given(questService.findUncompletedQuests(memberId)).willReturn(List.of(quest));

        // when & then
        mockMvc.perform(get("/api/v1/dashboard")
                        .header("X-Member-Id", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet.name").value("알코믹"))
                .andExpect(jsonPath("$.pet.level").value(1))
                .andExpect(jsonPath("$.quests[0].title").value("알고리즘 1문제 풀기"))
                .andExpect(jsonPath("$.quests[0].status").value("IN_PROGRESS")); // 퀘스트 상태값 검증
    }
}