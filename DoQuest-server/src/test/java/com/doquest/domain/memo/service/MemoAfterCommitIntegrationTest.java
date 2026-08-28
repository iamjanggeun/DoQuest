package com.doquest.domain.memo.service;

import com.doquest.domain.ai.client.AiClient;
import com.doquest.domain.ai.dto.AiParserDto;
import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.pet.entity.Pet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("Memo AFTER_COMMIT 통합 테스트")
class MemoAfterCommitIntegrationTest {

    @Autowired
    private MemoService memoService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private AiClient aiClient;

    private Member member;

    @BeforeEach
    void setUp() {
        reset(aiClient);
        String uniqueEmail = "after-commit-" + System.nanoTime() + "@example.com";
        member = memberRepository.save(Member.createMember(
                uniqueEmail,
                "password",
                "tester",
                Pet.createDefaultPet("test-pet")
        ));
    }

    @Test
    @DisplayName("메모 커밋 후 AI 호출과 파싱 완료 갱신이 실행된다")
    void committedMemo_TriggersAiAndMarksParsed() throws InterruptedException {
        CountDownLatch aiCalled = new CountDownLatch(1);
        AtomicBoolean memoVisibleWhenAiCalled = new AtomicBoolean(false);

        given(aiClient.parseMemo(anyLong(), anyLong(), anyString())).willAnswer(invocation -> {
            Long memoId = invocation.getArgument(0);
            memoVisibleWhenAiCalled.set(memoRepository.findById(memoId).isPresent());
            aiCalled.countDown();
            return successfulResponse();
        });

        Long memoId = memoService.createMemo(member.getId(), "내일 통합 테스트 결과 확인");

        assertThat(aiCalled.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(memoVisibleWhenAiCalled).isTrue();
        assertThat(awaitParsed(memoId)).isTrue();
    }

    @Test
    @DisplayName("AI 장애가 발생해도 커밋된 메모는 유지되고 isParsed는 false다")
    void aiFailure_DoesNotRollbackCommittedMemo() throws InterruptedException {
        CountDownLatch aiCalled = new CountDownLatch(1);
        given(aiClient.parseMemo(anyLong(), anyLong(), anyString())).willAnswer(invocation -> {
            aiCalled.countDown();
            throw new IllegalStateException("FastAPI unavailable");
        });

        Long memoId = memoService.createMemo(member.getId(), "AI 장애 격리 테스트");

        assertThat(aiCalled.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(memoRepository.findById(memoId))
                .isPresent()
                .get()
                .extracting(Memo::isParsed)
                .isEqualTo(false);
    }

    @Test
    @DisplayName("메모 생성 트랜잭션이 롤백되면 AI 리스너는 실행되지 않는다")
    void rolledBackMemo_DoesNotTriggerAi() throws InterruptedException {
        AtomicLong rolledBackMemoId = new AtomicLong();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            rolledBackMemoId.set(memoService.createMemo(member.getId(), "롤백 이벤트 테스트"));
            status.setRollbackOnly();
        });

        Thread.sleep(300);
        assertThat(memoRepository.findById(rolledBackMemoId.get())).isEmpty();
        verify(aiClient, never()).parseMemo(anyLong(), anyLong(), anyString());
    }

    private AiParserDto.Response successfulResponse() {
        return new AiParserDto.Response(
                true,
                "통합 테스트 결과 확인",
                "2026-08-29",
                null,
                "AFTER_COMMIT 검증",
                List.of()
        );
    }

    private boolean awaitParsed(Long memoId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (memoRepository.findById(memoId).map(Memo::isParsed).orElse(false)) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
