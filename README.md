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
  - [기존] DB ➔ MemoRepository ➔ MemoService (List<Memo> 엔티티 반환) ➔ MemoController (.map(MemoResponse::from) 변환) ➔ 클라이언트 (DTO 응답)
  - [현재]DB ➔ MemoRepository ➔ MemoService (.map(MemoResponse::from) DTO 변환) ➔ MemoController (List<MemoResponse> 그대로 전달) ➔ 클라이언트 (DTO 응답)
</details>