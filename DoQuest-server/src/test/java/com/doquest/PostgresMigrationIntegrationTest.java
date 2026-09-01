package com.doquest;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.entity.MemoAnalysis;
import com.doquest.domain.memo.repository.MemoAnalysisRepository;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.memo.service.MemoAnalysisService;
import com.doquest.domain.pet.entity.Pet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-must-be-at-least-32-bytes-long",
        "jwt.access-token-expiration=3600000"
})
@ActiveProfiles("postgres-test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private MemoAnalysisRepository memoAnalysisRepository;

    @Autowired
    private MemoAnalysisService memoAnalysisService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywaySchemaIsCompatibleWithJpaMappings() {
        Member saved = memberRepository.saveAndFlush(Member.createMember(
                "postgres@example.com",
                "password",
                "postgres-tester",
                Pet.createDefaultPet("postgres-pet")
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Integer retryColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'memo_analyses'
                  AND column_name IN ('attempt_count', 'last_error')
                """, Integer.class);
        Integer scheduleMemoUniqueConstraint = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'schedules'
                  AND constraint_name = 'uk_schedules_memo'
                  AND constraint_type = 'UNIQUE'
                """, Integer.class);

        assertThat(retryColumns).isEqualTo(2);
        assertThat(scheduleMemoUniqueConstraint).isEqualTo(1);
    }

    @Test
    void failedAnalysisMetadataIsPersistedAndClearedOnRestart() {
        Member member = memberRepository.saveAndFlush(Member.createMember(
                "retry-postgres@example.com",
                "password",
                "retry-postgres-tester",
                Pet.createDefaultPet("retry-postgres-pet")
        ));
        Memo memo = memoRepository.saveAndFlush(Memo.createMemo(member, "장애 복구 통합 테스트"));
        MemoAnalysis analysis = memoAnalysisRepository.saveAndFlush(MemoAnalysis.pending(memo));

        memoAnalysisService.failAnalysis(memo.getId(), 3, "connection refused\nstack detail");

        var failedMetadata = jdbcTemplate.queryForMap("""
                SELECT status, attempt_count, last_error
                FROM memo_analyses
                WHERE memo_analysis_id = ?
                """, analysis.getId());

        assertThat(failedMetadata.get("status")).isEqualTo("FAILED");
        assertThat(failedMetadata.get("attempt_count")).isEqualTo(3);
        assertThat(failedMetadata.get("last_error")).isEqualTo("connection refused stack detail");

        transactionTemplate.executeWithoutResult(status -> {
            MemoAnalysis failedAnalysis = memoAnalysisRepository.findById(analysis.getId()).orElseThrow();
            failedAnalysis.restart();
            memoAnalysisRepository.flush();
        });

        var restartedMetadata = jdbcTemplate.queryForMap("""
                SELECT status, attempt_count, last_error
                FROM memo_analyses
                WHERE memo_analysis_id = ?
                """, analysis.getId());

        assertThat(restartedMetadata.get("status")).isEqualTo("PENDING");
        assertThat(restartedMetadata.get("attempt_count")).isEqualTo(0);
        assertThat(restartedMetadata.get("last_error")).isNull();
    }
}
