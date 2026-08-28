# AI 후처리 동기·비동기 성능 및 장애 격리 검증

## 1. 실험 목적

메모 생성 과정에서 외부 LLM 호출을 요청 스레드와 분리했을 때 사용자 응답 지연이 얼마나 감소하는지 측정했다. 또한 `AFTER_COMMIT` 이벤트가 메모 저장 트랜잭션과 AI 후처리 장애를 실제로 분리하는지 검증했다.

이 실험에서 검증하려는 대상은 LLM 추론 속도 자체가 아니다. 동일한 FastAPI 및 OpenAI 호출을 유지한 상태에서 Spring의 비동기 실행 여부만 변경하여, 외부 I/O를 HTTP 응답 경로에서 제거한 효과를 측정했다.

## 2. 측정 환경과 통제 변수

| 항목 | 내용 |
|---|---|
| 측정일 | 2026-08-28 |
| 실행 환경 | 로컬 개발 환경 |
| Spring Boot | Java 17, Spring Boot 3.5.16 |
| 데이터베이스 | H2 인메모리 DB, MySQL 호환 모드 |
| AI 서비스 | FastAPI + LangChain LCEL + OpenAI |
| 측정 API | `POST /api/v1/memos` |
| 표본 수 | Sync 10회, Async 10회 |
| 변경 변수 | `MemoAiEventListener.handleMemoCreated()`의 `@Async("aiTaskExecutor")` 적용 여부 |
| 고정 조건 | `AFTER_COMMIT`, `REQUIRES_NEW`, RestClient, FastAPI `ainvoke()`, OpenAI 호출 구조 유지 |
| 완료 확인 | 반환된 `memoId`의 `isParsed=true` 여부를 50ms 간격으로 확인 |

동기 baseline은 `benchmark/sync-ai-call` 브랜치에서 `@Async("aiTaskExecutor")`만 제거하여 구성했다. FastAPI의 비동기 코드는 변경하지 않았다.

## 3. 측정 지표

- HTTP 응답 시간: 클라이언트가 메모 생성 요청을 시작한 시점부터 `201 Created` 응답을 받을 때까지의 시간
- AI 완료 시간: 메모 생성 요청을 시작한 시점부터 해당 Memo의 `isParsed`가 `true`로 반영될 때까지의 시간
- p95: 10개 표본을 inclusive percentile 방식으로 계산한 95번째 백분위수

## 4. 측정 결과

### 4.1 요약

| 지표 | Sync | Async | 변화 |
|---|---:|---:|---:|
| 평균 HTTP 응답 시간 | 1,761.55ms | 9.46ms | 99.46% 감소 |
| p50 HTTP 응답 시간 | 1,667.44ms | 7.41ms | 99.56% 감소 |
| p95 HTTP 응답 시간 | 2,097.04ms | 19.50ms | 99.07% 감소 |
| 최소 HTTP 응답 시간 | 1,477.18ms | 4.80ms | - |
| 최대 HTTP 응답 시간 | 2,110.69ms | 23.35ms | - |
| Async 평균 AI 완료 시간 | - | 2,191.98ms | 별도 후처리 |
| Async p50 AI 완료 시간 | - | 2,114.65ms | 별도 후처리 |
| Async p95 AI 완료 시간 | - | 2,848.39ms | 별도 후처리 |

### 4.2 비동기 원시 측정값

| 회차 | HTTP 응답 시간 | AI 완료 시간 |
|---:|---:|---:|
| 1 | 23.354ms | 1,858.249ms |
| 2 | 14.794ms | 2,509.924ms |
| 3 | 8.526ms | 3,115.943ms |
| 4 | 8.592ms | 2,521.387ms |
| 5 | 7.597ms | 2,143.216ms |
| 6 | 7.091ms | 1,911.872ms |
| 7 | 7.224ms | 1,588.660ms |
| 8 | 6.047ms | 2,203.961ms |
| 9 | 6.560ms | 2,086.078ms |
| 10 | 4.802ms | 1,980.466ms |

### 4.3 동기 원시 측정값

동기 방식에서는 AI 호출과 `isParsed=true` 반영이 완료된 후 HTTP 응답이 반환됐다. 모든 회차에서 응답 직후 `isParsed=true`를 확인했다.

| 회차 | HTTP 응답 시간 | 응답 직후 `isParsed` |
|---:|---:|:---:|
| 1 | 2,110.690ms | true |
| 2 | 1,554.294ms | true |
| 3 | 1,477.178ms | true |
| 4 | 1,896.368ms | true |
| 5 | 1,999.529ms | true |
| 6 | 1,691.841ms | true |
| 7 | 1,637.228ms | true |
| 8 | 2,080.346ms | true |
| 9 | 1,643.040ms | true |
| 10 | 1,524.951ms | true |

## 5. 결과 해석

`@Async("aiTaskExecutor")` 적용 후 평균 HTTP 응답 시간은 1,761.55ms에서 9.46ms로 감소했다. 이는 약 99.46%의 응답 지연 감소다.

반면 비동기 방식의 AI 완료 시간은 평균 2,191.98ms였다. 따라서 이 결과는 LLM 추론 성능이 개선됐다는 의미가 아니다. 외부 LLM I/O를 사용자 요청 경로에서 분리하여 HTTP 응답을 먼저 반환하도록 만든 효과다.

포트폴리오에서는 다음과 같이 표현한다.

> `@TransactionalEventListener(AFTER_COMMIT)`와 전용 `aiTaskExecutor`를 적용해 외부 LLM I/O를 메모 생성 응답 경로에서 분리했다. 실제 OpenAI E2E를 동기·비동기 각각 10회 측정한 결과, 평균 HTTP 응답 시간이 1,761.55ms에서 9.46ms로 약 99.46% 감소했다. AI 완료 시간은 별도로 측정하여 추론 시간 단축이 아닌 요청 지연 격리 효과임을 확인했다.

## 6. AFTER_COMMIT 통합 테스트

실제 Spring 트랜잭션 관리자와 H2를 사용하는 통합 테스트로 다음 시나리오를 검증했다.

| 시나리오 | 검증 결과 |
|---|---|
| 메모 생성 트랜잭션 커밋 | 커밋 후 AI Client 호출 및 `isParsed=true` 반영 |
| AI Client 예외 | Memo row 유지, `isParsed=false` 유지 |
| 메모 생성 트랜잭션 롤백 | Memo row 미생성, AI Client 호출 없음 |

관련 통합 테스트는 `MemoAfterCommitIntegrationTest`이며, 커밋은 `3c2ae62`다.

## 7. 실제 FastAPI-down 장애 실험

FastAPI 프로세스만 종료하고 원래 비동기 Spring 서버에서 메모 생성 요청을 전송했다.

| 확인 항목 | 결과 |
|---|---|
| Memo 생성 HTTP 상태 | `201 Created` |
| HTTP 응답 시간 | 13.417ms |
| 생성된 Memo ID | 12 |
| Memo row | 유지 |
| `isParsed` | `false` |
| Spring 로그인 API | `200 OK` |
| 장애 범위 | AI 후처리 실패로 제한 |

이 실험은 외부 AI 장애가 핵심 Memo 저장 트랜잭션으로 전파되지 않는다는 것을 보여준다. 다만 현재 구조에는 retry, outbox 또는 DLQ가 없으므로 실패한 AI 작업의 자동 복구까지 보장하지는 않는다.

## 8. 재현 근거와 한계

- 동기 비교 브랜치: `benchmark/sync-ai-call`
- 동기 baseline 커밋: `09c4558`
- `AFTER_COMMIT` 통합 테스트 커밋: `3c2ae62`
- 모든 Spring 테스트 통과
- 실제 OpenAI 호출은 네트워크와 모델 응답 편차의 영향을 받음
- 표본 수가 각 10회이므로 대규모 부하 테스트 결과가 아닌 로컬 E2E 비교 실험으로 한정함
- 비동기 AI 완료 시간은 50ms polling 간격만큼의 측정 오차가 있을 수 있음

따라서 이 결과는 운영 환경의 절대 성능 수치가 아니라, 동일한 로컬 환경에서 Spring 실행 방식 하나만 변경했을 때 나타난 상대적인 응답 지연 개선 근거로 사용한다.
