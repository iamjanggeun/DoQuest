# PostgreSQL AFTER_COMMIT 및 FastAPI 장애 격리 검증

## 1. 실험 목적

H2에서 검증했던 `AFTER_COMMIT` 비동기 흐름을 실제 PostgreSQL에서도 재검증하고, FastAPI 연결 장애가 Memo 저장 및 Spring API 가용성으로 전파되지 않는지 확인한다.

## 2. 실험 환경

| 항목 | 내용 |
|---|---|
| 실험 일시 | 2026-08-31 KST |
| Spring | Spring Boot 3.5.16, Java 17 |
| Database | PostgreSQL 16.15 Docker container |
| Schema | Flyway `V1__init_schema.sql` |
| JPA | Hibernate `ddl-auto=validate` |
| 자동 검증 | JUnit 5 + Testcontainers + Mockito |
| HTTP 검증 | curl (동일 요청을 Postman으로 재현 가능) |

JWT, DB 비밀번호와 API 키 원문은 기록하지 않는다.

## 3. PostgreSQL 자동 통합 테스트

### 3.1 Flyway 및 JPA 매핑

`PostgresMigrationIntegrationTest`에서 다음을 검증했다.

- `postgres:16-alpine` 컨테이너 기동
- 빈 `public` 스키마에 Flyway V1 적용
- Hibernate 엔티티 매핑 검증
- Member와 Pet 실제 저장
- UTC `Instant` 감사 시각 생성

결과:

```text
tests=1, skipped=0, failures=0, errors=0
PostgreSQL 16.15
Successfully applied 1 migration to schema "public", now at version v1
```

### 3.2 AFTER_COMMIT 트랜잭션 경계

`MemoAfterCommitIntegrationTest`를 PostgreSQL Testcontainers 환경으로 전환하고 다음 세 시나리오를 검증했다.

| 시나리오 | 검증 결과 |
|---|---|
| 분석 요청 트랜잭션 커밋 | 커밋 후 AI Client 호출, 호출 시 Memo 조회 가능, 분석 성공 반영 |
| AI Client 예외 | Memo 유지, `isParsed=false`, MemoAnalysis `FAILED` |
| 분석 요청 트랜잭션 롤백 | Memo 미저장, AI Client 미호출 |

결과:

```text
tests=3, skipped=0, failures=0, errors=0
```

## 4. FastAPI 연결 장애 HTTP E2E

### 4.1 격리 구성

사용자가 실행 중이던 서비스는 중단하지 않고 별도 포트로 실험 환경을 구성했다.

```text
PostgreSQL: localhost:55432
Spring:     localhost:18080
FastAPI:    localhost:18000 (의도적으로 미기동)
```

Spring은 `prod` 프로파일로 실행했으며 PostgreSQL 접속, Flyway V1 검증, Hibernate 매핑 검증 후 정상 기동했다.

### 4.2 Memo 저장

```http
POST /api/v1/memos
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "content": "내일 오후 7시 강남에서 PostgreSQL 장애 격리 실험"
}
```

| 항목 | 결과 |
|---|---|
| HTTP | `201 Created` |
| 응답 시간 | `100.205ms` |
| memoId | `1` |

### 4.3 AI 분석 요청

```http
POST /api/v1/memos/1/analysis
Authorization: Bearer <ACCESS_TOKEN>
```

결과:

```json
{
  "memoId": 1,
  "status": "PENDING",
  "isSchedule": false,
  "title": null,
  "scheduledAt": null,
  "scheduledTime": null,
  "location": null,
  "summaryInfo": null
}
```

| 항목 | 결과 |
|---|---|
| HTTP | `202 Accepted` |
| 응답 시간 | `66.748ms` |
| 초기 상태 | `PENDING` |

### 4.4 비동기 실패 반영

비동기 작업은 `AI-Worker-1`에서 실행됐고 연결되지 않은 FastAPI 주소에 요청한 뒤 다음 로그를 남겼다.

```text
[AI 파이프라인 장애 격리] memoId=1 분석 실패
Cause: Connection refused
```

후속 조회 결과:

| 확인 항목 | 결과 |
|---|---|
| 분석 조회 HTTP | `200 OK` |
| 분석 상태 | `FAILED` |
| Memo 목록 HTTP | `200 OK` |
| Memo row | 유지 |
| `isParsed` | `false` |
| 로그인 재요청 | `200 OK` |
| Spring 생존 | 정상 |

상태 전이:

```text
PENDING → FAILED
```

## 5. FastAPI 복구 후 수동 재시도

같은 PostgreSQL 데이터를 유지하고 Spring의 AI 주소를 정상 FastAPI인 `localhost:8000`으로 변경한 뒤 동일 분석을 다시 요청했다.

재요청 결과:

| 항목 | 결과 |
|---|---|
| 분석 재요청 | `202 Accepted`, `PENDING` |
| 재요청 응답 시간 | `215.472ms` |
| 최종 조회 | `200 OK`, `SUCCEEDED` |
| 일정 여부 | `true` |
| 날짜 | `2026-09-01` |
| 시간 | `19:00` |
| 장소 | `강남` |

상태 전이:

```text
FAILED → PENDING → SUCCEEDED
```

이는 사용자의 명시적 재요청에 의한 복구다. 자동 retry, outbox와 DLQ는 아직 구현하지 않았다.

## 6. 실험 중 발견하고 수정한 문제

운영 프로파일에서 `PathRequest.toH2Console()`이 H2 콘솔 전용 빈을 조회하면서 모든 HTTP 요청이 `500 Internal Server Error`가 되는 문제를 발견했다.

H2 콘솔 매처를 문자열 경로 `/h2-console/**`로 제한해 PostgreSQL 운영 프로파일이 H2 전용 빈에 의존하지 않도록 수정했다. 수정 후 회원가입, 로그인, Memo, 분석 API가 정상 동작했다.

## 7. 최종 판정

```text
PostgreSQL Flyway V1: PASS
Hibernate validate: PASS
AFTER_COMMIT 커밋/롤백: PASS
AI 실패 상태 기록: PASS
HTTP 장애 격리: PASS
Memo 영속성: PASS
Spring API 생존: PASS
FastAPI 복구 후 수동 재시도: PASS
```

외부 AI 장애는 Memo 저장 트랜잭션과 Spring API 가용성으로 전파되지 않았다. PostgreSQL 환경에서도 `AFTER_COMMIT`, 비동기 워커와 독립 결과 트랜잭션의 경계가 의도대로 동작했다.
