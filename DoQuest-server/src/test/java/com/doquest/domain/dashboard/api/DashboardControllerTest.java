package com.doquest.domain.dashboard.api;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.pet.entity.Pet;
import com.doquest.domain.pet.service.PetService;
import com.doquest.domain.quest.entity.Quest;
import com.doquest.domain.quest.entity.QuestCategory;
import com.doquest.domain.quest.service.QuestService;
import com.doquest.global.config.security.JwtAuthenticationFilter;
import com.doquest.global.config.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false) // WebMvc 단위 슬라이스 테스트 격리
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private QuestService questService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private void setMockAuthentication(Long memberId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("대시보드 조회 성공 - 200 OK")
    void getDashboard_성공() throws Exception {
        // given
        Long memberId = 1L;
        setMockAuthentication(memberId);

        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Pet pet = Pet.createDefaultPet("알코믹");
        Quest quest = Quest.createQuest(member, "알고리즘 1문제 풀기", QuestCategory.STUDY, 20);

        given(petService.getPetByMemberId(memberId)).willReturn(pet);
        given(questService.findUncompletedQuests(memberId)).willReturn(List.of(quest));

        // when & then
        mockMvc.perform(get("/api/v1/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet.name").value("알코믹"))
                .andExpect(jsonPath("$.pet.level").value(1))
                .andExpect(jsonPath("$.pet.stage").value("알")) // PetStage description 검증
                .andExpect(jsonPath("$.quests[0].title").value("알고리즘 1문제 풀기"))
                .andExpect(jsonPath("$.quests[0].status").value("IN_PROGRESS")); // QuestStatus Enum 검증
    }
}