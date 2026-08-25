# Revision History

| 개정일자 | 버전 | 주요 변경 및 반영 내용 | 작성자 |
| :--- | :--- | :--- | :--- |
| 2026.08.08 | v0.1.0 | Initial Project Setup & Domain Entity Design (Member, Pet, Quest) | Janggeun |
| 2026.08.09 | v0.2.0 | Dashboard Aggregator API (`DashboardController`, `DashboardResponse`) 설계 | Janggeun |
| 2026.08.10 | v0.3.0 | 펫 경험치 어뷰징 방지 가드레일(`QuestStatus`, `startedAt` 30분 타이머) 구축 및 단방향 1:1 영속성 전이 최적화 | Janggeun |
| 2026.08.12 | v0.4.0 | Memo 도메인 CRUD API 및 Java 17 `record` 기반 DTO/응답 스펙 설계 | Janggeun |
| 2026.08.13 | v0.4.1 | `@WebMvcTest` 슬라이스 테스트 환경 구축 및 `MethodArgumentNotValidException` 핸들링을 통한 HTTP 예외 응답 정합성(400 vs 500) 교정 (`JpaConfig` 분리) | Janggeun |
| 2026.08.14 | v0.5.0 | Spring Security 6.x + JJWT 기반 Stateless 인증 인프라 구축 (`JwtProvider`, `JwtAuthenticationFilter`, `application-secret.yml` 환경 격리) | Janggeun |
| 2026.08.17 | v0.5.1 | Memo 도메인 계층 격리(Service DTO 반환) 및 `@AuthenticationPrincipal` 전환, 테스트 표준화(`@MockitoBean`) | Janggeun |
| 2026.08.18 | v0.6.0 | FastAPI + LangChain LCEL 기반 비동기 AI 서빙 엔진(doquest-ai) 구축 및 어댑터 패턴 기반 슬라이스 테스트(pytest) 작성 | Janggeun |
| 2026.08.19 | v0.6.1 | Spring 6 RestClient 통신 계층 구현 및 MockRestServiceServer 바인딩 트러블슈팅을 통한 외부 의존성 0% 슬라이스 테스트(AiClientTest) 완성 | Janggeun |
| 2026.08.20 | v0.7.0 | Spring Event 기반 비동기 AI 파이프라인 구축 (AsyncConfig 전용 스레드 풀 격리, @TransactionalEventListener(AFTER_COMMIT) 및 내결함성 단위 검증 완료) | Janggeun |
| 2026.08.21 | v0.7.1 | Spring Event 기반 비동기 AI 파이프라인 구축 및 E2E 검증 | Janggeun |
| 2026.08.24 | v0.8.0 | FastAPI 프롬프트 강화 및 KST(Asia/Seoul) 추가 | Janggeun |
| 2026.08.24 | v0.8.1 | Spring <-> FastAPI 간 DTO / Schemas 불일치 수정 | Janggeun |
| 2026.08.25 | v0.9.0 | • **Schedule 도메인 구축**: 캘린더 기능 추가<br>• **복합 인덱스 기반 D-3 큐레이션 최적화**: LLM 반복 호출 없이 `idx_schedules_member_date` 복합 인덱스 쿼리로 마감 3일 이내 데이터 즉시 필터링<br>• **Two-Phase Commit UX 지원**: 비정형 메모 파싱 후 즉시 저장하지 않고, 유저 확인을 거쳐 `ScheduleCreateRequest`로 영속화 | Janggeun |

---

# DoQuest (AI 기반 생산성 & 게이미피케이션 습관 형성 서비스)

> **"오늘의 메모가 내일의 퀘스트가 되는 개인화 생산성 플랫폼"**  
> DoQuest는 단순한 To-Do 리스트를 넘어, 메모 작성만으로 AI가 일정을 자동 추출하고 게이미피케이션(펫 성장) 요소를 결합하여 사용자의 지속적인 동기부여를 이끌어내는 백엔드 서비스입니다.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.x, Spring Data JPA
- **Database:** MySQL 8.0, Redis (예정)
- **AI Engine (Phase 4 예정):** Python, FastAPI, LangChain, Vector DB (Chroma/pgvector)
- **Testing & Tools:** JUnit5, AssertJ, Mockito, Postman, Git/GitHub

---

## Architecture & Core Design Principles

### 1. BFF (Backend For Frontend) / Aggregator Pattern
- **네트워크 Latency 및 Round-Trip 최적화:** 1번 대시보드 화면 진입 시, 펫 정보(`Pet`)와 미완료 퀘스트 목록(`Quest`)을 각각 분리된 API로 요청하지 않고 `DashboardController`에서 통합 조회하여 하나의 `DashboardResponse` DTO로 묶어 제공합니다.

### 2. 도메인 주도 설계 (DDD) & 계층 간 책임 분리
- **신뢰 경계 (Trust Boundary) 구축:** 퀘스트 생성 시 보상 경험치(`rewardExp`)를 클라이언트 요청(Request DTO)으로 받지 않고, 오직 서버 비즈니스 레이어(`QuestService`)에서 상수로 관리 및 주입하여 데이터 위변조(어뷰징)를 원천 차단했습니다.
- **Single Source of Truth:** `boolean isCompleted` 플래그 대신 `QuestStatus` Enum (`IN_PROGRESS`, `COMPLETED`)으로 상태 관리를 일원화하여 데이터 불일치를 방지했습니다.

---

## Key Abusing Guardrails (어뷰징 방지 가드레일)

### 30분 최소 수행 시간 타임스탬프 가드레일 (Rate Limiting)
* **문제 정의:** 유저가 펫 경험치를 빠르게 올리기 위해 퀘스트 생성 직후 1초 만에 완료 버튼을 연속 연타하는 경험치 어뷰징 위험성 존재.
* **시행착오 & UX 개선:** 
  - *초기 안:* 퀘스트 생성 자체에 1분 쿨타임을 두려 했으나, 당일 할 일을 한꺼번에 등록하는 실제 사용자의 UX를 심각하게 저해함을 인지.
  - *개선 안:* **등록과 수행 이벤트의 분리.** 퀘스트 생성 시점(`startedAt = LocalDateTime.now()`)을 기록하고, **완료 처리 시 `startedAt` 기준 최소 30분이 지난 경우에만 완료 승인 및 경험치를 지급**하도록 도메인 가드레일(`validateCompleteCooldown`) 구축.

---

## Performance Optimization & Indexing

### 1. 복합 인덱스 (`Composite Index`) 적용
- **조회 패턴:** 대시보드 진입 시 `WHERE member_id = ? AND status = 'IN_PROGRESS'` 조건의 조회가 빈번히 발생함.
- **최적화:** `@Index(name = "idx_quests_member_status", columnList = "member_id, status")` 복합 인덱스를 생성하여 테이블 풀 스캔(Full Scan)을 방지하고 B-Tree 인덱스 스캔 효율 극대화.

### 2. 읽기 전용 트랜잭션 최적화
- `PetService`, `QuestService` 내 단건/목록 조회 메서드에 `@Transactional(readOnly = true)`를 선언하여 영속성 컨텍스트의 스냅샷 보관 비용 및 플러시(Flush) 오버헤드를 제거하여 CUD 대비 조회 성능 향상.

---

## Testing Strategy

- **Domain Unit Test (`QuestTest`, `PetTest`):** Mocking 없이 팩토리 메서드 및 상태 변이 비즈니스 로직의 순수 자바 단위 검증.
- **Service Layer Test (`QuestServiceTest`, `PetServiceTest`):** Mockito 기반의 계층 격리 테스트.
  - `ReflectionTestUtils`를 활용해 H2/DB 인프라 연결 없이 `startedAt` 시각을 35분 전으로 가상 조작하여 30분 쿨타임 가드레일의 경계값(Edge Case) 및 타인 퀘스트 완료 시도(보안 예외)를 1초 미만의 속도로 고속 검증.

---

## Troubleshooting & Engineering Decisions

<details>
<summary><b>1. Member와 Pet 간의 1:1 단방향 매핑 및 순환 참조(닭과 달걀) 해결</b></summary>

- **문제:** `Member`와 `Pet`을 양방향 1:1로 매핑할 경우, 생성 시점에 "누구를 먼저 save 해야 하는가"에 대한 순환 의존성이 발생.
- **해결:** `Pet`은 `Member`의 존재를 모르는 순수 독립 도메인(`createDefaultPet`)으로 설계하고, `Member`가 `pet_id` FK 및 영속성 전이(`CascadeType.PERSIST`)를 단독 관리하는 **단방향 1:1 매핑**으로 결합도를 축소.
</details>

<details>
<summary><b>2. OCP(개방-폐쇄 원칙)를 고려한 퀘스트 보상 경험치 설계</b></summary>

- **문제:** 현재는 퀘스트 완료 시 20 EXP 고정 지급이지만, 향후 Phase 4에서 **'AI 기반 난이도 동적 측정'**이 들어올 예정.
- **해결:** `Quest.createQuest` 팩토리 메서드가 보상치를 파라미터로 주입받도록 설계하여, 향후 AI RAG 파이프라인 연동 시 엔티티 코드 수정 없이 `QuestService` 로직 변경만으로 유연하게 확장 가능하도록 OCP 준수.
</details>

<details>
<summary><b>3. @WebMvcTest 기반 슬라이스 테스트를 통한 입력 검증 예외(400 vs 500) 조기 발견 및 교정</b></summary>

- **문제 상황 (Problem):**
  - 메모 생성 API(`POST /api/v1/memos`) 개발 후 `@Valid` 검증 실패(공백 문자열 입력) 테스트 작성 중, 예상했던 `400 Bad Request` 대신 `500 Internal Server Error`가 반환되는 현상 발견.
- **원인 분석 (Root Cause):**
  - Spring MVC는 DTO 유효성 검증 실패 시 `MethodArgumentNotValidException`을 발생시킴.
  - 전역 예외 처리기(`GlobalExceptionHandler`)에 해당 예외 전용 핸들러가 누락되어 최상위 `Exception.class` 핸들러로 인계되었고, 이로 인해 서버 내부 오류(500)로 오인 응답됨.
- **해결 방안 (Action):**
  - `@RestControllerAdvice`에 `MethodArgumentNotValidException` 전용 `@ExceptionHandler`를 선언하고, `ErrorCode.INVALID_INPUT_VALUE`와 함께 HTTP `400 Bad Request` 상태 코드를 명시하도록 정합성 교정.
  - `@EnableJpaAuditing`을 메인 클래스에서 독립 `JpaConfig`로 분리하여 웹 슬라이스 테스트(`@WebMvcTest`) 컨텍스트 격리성 확보.
- **성과 및 이점 (Result):**
  - 실제 서버를 실행하거나 E2E 테스트를 거치지 않고 **0.5초 만에 수행되는 `@WebMvcTest` 슬라이스 테스트 환경에서 API 응답 규격 결함을 조기에 발견(Shift-Left)**하여 배포 전 리스크 최소화.
</details>

<details>
<summary><b>4. 계층 간 결합도 축소를 위한 DTO 반환 리팩토링 및 테스트 코드 동기화</b></summary>

- **문제 상황 (Problem):**
  - `MemoService`가 영속성 엔티티(`Memo`)를 Presentation 계층(Controller)으로 직접 반환하고 있어 도메인 계층과 웹 계층 간 강한 결합(Tight Coupling)이 발생.
  - Spring Security 적용 및 API 응답 스펙을 명확히 하기 위해 Service 반환 타입을 DTO(`MemoResponse`)로 전환하는 과정에서, 기존 단위/슬라이스 테스트(`MemoServiceTest`, `MemoControllerTest`)에 타입 불일치 컴파일 에러 및 스펙 불일치 테스트 실패 다수 발생.
- **원인 분석 (Root Cause):**
  - 엔티티의 변경이 Presentation 계층의 JSON 응답 스펙에 즉각적인 영향을 주는 구조였음.
  - 기존 테스트 코드 내 Mocking Stubbing(`willReturn`) 및 단언(Assertion) 로직이 엔티티 인스턴스에 강하게 의존하고 있어, 반환 타입 변경 시 테스트 코드가 연쇄적으로 깨짐.
- **해결 방안 (Action):**
  - `MemoResponse` Record 내부에 정적 팩토리 메서드(`from(Memo memo)`)를 구현하여 엔티티 to DTO 변환 책임을 캡슐화.
  - `MemoService` 내부에서 DTO 변환을 완료하여 Controller로 전달하도록 계층 간 역할을 명확히 분리.
  - `MemoRepository`의 최신순 정렬 메서드(`findByMemberIdOrderByCreatedAtDesc`) 호출부와 Service 로직의 정합성을 동기화.
  - `MemoServiceTest`의 검증 대상을 DTO 필드 단언으로 수정하고, Spring Boot 최신 테스트 표준인 `@MockitoBean`을 적용하여 전체 테스트 성공
- **성과 및 이점 (Result):**
  - 도메인 엔티티의 내부 구현 변경이 외부 API 스펙(DTO)으로 전파되지 않도록 계층 간 독립성 확보.
  - 계층 분리 리팩토링 과정에서 단위/슬라이스 테스트를 함께 동기화하여 변경에 안전한 테스트 코드베이스 구축.

```text
  [기존 (As-Is)]
  DB ──▶ MemoRepository ──▶ MemoService (List<Memo> 반환) ──▶ MemoController (DTO 변환) ──▶ Client

  [개선 (To-Be)]
  DB ──▶ MemoRepository ──▶ MemoService (List<MemoResponse> DTO 변환) ──▶ MemoController ──▶ Client
```
</details>

<details>
<summary><b>5. Spring 6 RestClient 슬라이스 테스트 격리 실패 이슈</b></summary>

- **문제 상황:**
  - `@RestClientTest` 실행 시 실제 타겟 서버(`localhost:8000`)로의 연결을 시도하여 `Connection refused` 발생.
- **원인 분석:** 
  - `AiClientConfig`에서 타임아웃 설정을 위해 생성한 `SimpleClientHttpRequestFactory`가 `@RestClientTest`의 내부 Mock RequestFactory를 덮어써 가로채기(Mock) 실패.
- **해결 방안:**
  - `@BeforeEach`에서 `MockRestServiceServer.bindTo(restClientBuilder)`를 통해 Mocking 채널이 연결된 `RestClient`를 직접 빌드하여 `AiClient`에 수동 주입하도록 테스트   아키텍처 리팩토링.
- **결과:** 
  - 외부 AI 서버 의존성 0%, 0.2초 이내의 초고속 슬라이스 테스트 파이프라인 구축 완료.
</details>

<details>
<summary><b>6. 외부 AI I/O 블로킹 격리 및 비동기 E2E 검증</b></summary>

- **문제 상황:**
  - 메모 작성과 FastAPI 연동을 진행했을때 클라이언트 응답 시간이 오래 걸리는 문제
- **원인 분석:** 
  - 동기 방식으로 FastAPI(LLM 추론)를 호출할 경우, 외부 네트워크 지연(평균 1.8초)으로 인해 클라이언트 응답 시간이 과도하게 증가했음.
- **해결 방안:**
  - ApplicationEventPublisher를 도입하여 메모 쓰기 트랜잭션 커밋 직후 (AFTER_COMMIT) 비동기 이벤트 발행.
  - aiTaskExecutor 독립 스레드풀을 통해 RestClient 호출을 격리 처리함으로써 메인 스레드 블로킹 해소.
- **검증 결과:** 
  - Postman 기반 JWT 발급 → 메모 생성 → FastAPI AI 추론 → DB 후처리 전 과정 검증 성공.
  - 메모 저장 API 응답 속도 1,800ms → 130ms(Cold) / 20ms(Warm) 로 대폭 단축.
  - 외부 AI API 장애 발생 시에도 사용자 메모 원본 데이터의 영속성을 100% 보장하는 내결함성(Resilience) 확보.
```
비동기 이벤트 기반 아키텍처(EDA)를 통해 코어 비즈니스 트랜잭션과 외부 AI 통신을 물리적으로 분리함으로써, 시스템 가용성 보장과 레이턴시 90% 단축을 동시에 달성한 경험 (추후 삭제)
```
</details>

<details>
<summary><b>7. 분산 MSA 통신 규격 정합성 교정 및 KST 기반 시계열 LLM 가드레일 구축</b></summary>

- **문제 상황:**
  - Spring Boot와 FastAPI 간 비동기 메모 파싱 통신 시 역직렬화 누락으로 날짜(`scheduled_at`) 및 메타데이터가 `null`로 바인딩되는 결함 발생.
  - 서버 배포 환경(UTC) 기준 시차(9시간)로 인해 야간 시간대 "오늘/내일" 상대 시간 파싱 오차가 발생하고, RAG 검증망 부재로 가짜 URL(404 에러 링크)이 생성되는 환각 현상 확인.
- **원인 분석:** 
  - Spring DTO(`MemoAiParseResponse`, `AiParserDto`)와 FastAPI Pydantic 스키마(`ScheduleMetadata`) 간 네이밍 불일치 및 상관관계 ID(`memo_id`) 누락으로 비동기 분산 환경의 추적성(Traceability) 훼손.
  - `date.today()` 사용으로 인한 서버 로컬 타임존(UTC) 의존성 및 자유 생성 프롬프트의 신뢰 경계(Trust Boundary) 부재.
- **해결 방안:**
  - **Contract 정합성 동기화**: `memo_id`, `scheduled_at`, `location`, `summary_info`, `action_links` 규격으로 양측 스키마 1:1 일치화 및 Jackson Snake/Camel Case 바인딩 보정.
  - **Temporal Grounding 가드레일**: `ZoneInfo("Asia/Seoul")` 기반 KST 오늘 날짜 주입 함수를 LCEL 체인에 바인딩하여 24시간 정확한 절대 날짜(`YYYY-MM-DD`) 변환 보장.
  - **프롬프트 제약 및 할루시네이션 차단**: 검증되지 않은 임의 링크 생성을 원천 차단하고 구조화된 일정 메타데이터 추출에만 집중하도록 시스템 프롬프트 고도화.
- **검증 결과:** 
  - Spring ↔ FastAPI 간 비동기 페이로드 유실률 0% 달성 및 분산 상관관계 ID(`memo_id`) 기반 로그 추적성 확보.
  - 타임존 오차 없는 시계열 일정 파싱 정확도 확보 및 안전한 Schedule 도메인 연동 기반 완성.
</details>