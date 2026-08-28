# DoQuest Two-Phase E2E 테스트 결과

## 1. 문서 정보

- 테스트 일자: 2026-08-28
- 대상 기능: Memo AI 분석 결과 저장·조회·사용자 확정·Schedule 생성
- Spring 환경: Spring Boot 3.5.16, Java 17, H2 In-Memory
- AI 환경: FastAPI, LangChain LCEL, OpenAI Structured Output
- 운영 DB: 미선정·미연동
- 상태: 정상 Two-Phase 및 핵심 가드레일 HTTP E2E 통과

## 2. 검증 대상 흐름

```text
POST /api/v1/memos
    ↓
Memo + MemoAnalysis(PENDING) 저장
    ↓ AFTER_COMMIT + @Async
FastAPI /api/v1/ai/parse-memo 호출
    ├─ 성공 → MemoAnalysis(SUCCEEDED) + Memo.isParsed=true
    └─ 실패 → MemoAnalysis(FAILED) + Memo.isParsed=false
    ↓
GET /api/v1/memos/{memoId}/analysis
    ↓ 사용자 확인
POST /api/v1/memos/{memoId}/analysis/confirm
    ↓
Schedule 저장 + MemoAnalysis(CONFIRMED)
    ↓
GET /api/v1/schedules?year={year}&month={month}
```

## 3. 자동 테스트 기준선

Spring 전체 테스트 실행 결과:

```text
tests=47
skipped=0
failures=0
errors=0
BUILD SUCCESSFUL
```

자동 테스트에서 확인한 항목:

- Memo 생성 시 `MemoAnalysis(PENDING)` 저장
- AI 분석 성공 시 `SUCCEEDED` 전환 및 `Memo.isParsed=true`
- AI 분석 날짜 문자열을 `LocalDate`로 변환
- 분석 결과 확정 시 Schedule 생성
- 확정 완료 후 `CONFIRMED` 전환
- 동일 분석 결과 중복 확정 차단
- 타 회원 Memo를 Schedule에 연결하지 못하도록 소유권 검증
- AI 서버 오류 시 성공 처리 호출 방지 및 실패 상태 기록 요청
- 기존 Memo, Schedule, Quest, Pet, 인증 테스트 회귀 없음

## 4. 사전 상태 확인

최초 확인 당시에는 서버가 기동되지 않았으나, 2026-08-28 19:34 KST 실행 시 두 서비스가 정상 기동된 상태임을 확인했다.

| 서비스 | 확인 URL | 결과 |
|---|---|---|
| FastAPI | `http://127.0.0.1:8000/health` | `200 OK`, `{"status":"UP","service":"doquest-ai"}` |
| Spring | `http://127.0.0.1:8080` | 인증된 Memo/Schedule API 응답으로 정상 기동 확인 |

최초 `HTTP 000`은 기능 실패가 아니라 E2E 실행 전 서버가 기동되지 않은 상태였다.

## 5. 정상 Two-Phase E2E

### 5.1 메모 생성

요청:

```http
POST /api/v1/memos
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

```json
{
  "content": "2026년 8월 30일 선릉에서 기술 면접 준비하기"
}
```

| 확인 항목 | 기대 결과 | 실제 결과 |
|---|---|---|
| HTTP 상태 | `201 Created` | `201 Created` - PASS |
| memoId | 숫자 ID 반환 | `1` - PASS |
| Memo 저장 | 저장됨 | 목록 조회에서 memoId `1` 확인 - PASS |
| 초기 분석 상태 | `PENDING` | 1·2차 조회 모두 `PENDING` - PASS |

### 5.2 분석 결과 조회

```http
GET /api/v1/memos/{memoId}/analysis
Authorization: Bearer <ACCESS_TOKEN>
```

| 확인 항목 | 기대 결과 | 실제 결과 |
|---|---|---|
| status | `SUCCEEDED` | 3차 조회에서 `SUCCEEDED` - PASS |
| isSchedule | `true` | `true` - PASS |
| scheduledAt | `2026-08-30` | `2026-08-30` - PASS |
| location | `선릉` 포함 | `선릉` - PASS |
| Memo.isParsed | `true` | `true` - PASS |

실제 분석 결과:

```json
{
  "memoId": 1,
  "status": "SUCCEEDED",
  "isSchedule": true,
  "title": "기술 면접 준비",
  "scheduledAt": "2026-08-30",
  "location": "선릉",
  "summaryInfo": "선릉에서 기술 면접을 준비합니다."
}
```

첫 조회 시각부터 2초 간격으로 폴링했으며, 두 번의 `PENDING` 이후 세 번째 조회에서 `SUCCEEDED`가 관찰되었다. 전체 스크립트 시작부터 종료까지 약 4초가 소요됐다.

### 5.3 사용자 확정

```http
POST /api/v1/memos/{memoId}/analysis/confirm
Authorization: Bearer <ACCESS_TOKEN>
```

| 확인 항목 | 기대 결과 | 실제 결과 |
|---|---|---|
| HTTP 상태 | `201 Created` | `201 Created` - PASS |
| scheduleId | 숫자 ID 반환 | `1` - PASS |
| memoId | 원본 Memo ID와 동일 | `1` - PASS |
| 분석 상태 | `CONFIRMED` | 후속 조회에서 `CONFIRMED` - PASS |

### 5.4 캘린더 조회

```http
GET /api/v1/schedules?year=2026&month=8
Authorization: Bearer <ACCESS_TOKEN>
```

| 확인 항목 | 기대 결과 | 실제 결과 |
|---|---|---|
| 생성 일정 포함 | 포함됨 | scheduleId `1` 포함 - PASS |
| scheduledAt | `2026-08-30` | `2026-08-30` - PASS |
| isCompleted | `false` | `false` - PASS |
| 동일 memoId 일정 수 | 1개 | 1개 - PASS |

## 6. 가드레일 E2E

| 시나리오 | 기대 HTTP | 기대 코드 | 실제 결과 |
|---|---:|---|---|
| 동일 분석 재확정 | 409 | `MA003` | `409 / MA003` - PASS |
| 확정된 Schedule의 원본 Memo 삭제 | 409 | `MA004` | `409 / MA004` - PASS |
| 일정이 아닌 분석 결과 확정 | 409 | `MA002` | 실행 대기 |
| 다른 회원의 분석 결과 조회 | 404 | `MA001` | 실행 대기 |
| 다른 회원 Memo를 Schedule에 연결 | 400 | `C001` | 실행 대기 |

## 7. FastAPI 장애 E2E

조건:

```text
Spring ON
FastAPI OFF
H2 ON
```

| 확인 항목 | 기대 결과 | 실제 결과 |
|---|---|---|
| Memo 생성 HTTP 상태 | `201 Created` | 실행 대기 |
| Memo row | 유지 | 실행 대기 |
| 분석 상태 | `FAILED` | 실행 대기 |
| Memo.isParsed | `false` | 실행 대기 |
| Spring 생존 확인 | 로그인 또는 API 정상 응답 | 실행 대기 |
| 장애 범위 | AI 후처리로 제한 | 실행 대기 |

## 8. 판정 기준

최종 통과 조건:

- 정상 흐름이 `PENDING → SUCCEEDED → CONFIRMED`로 전이된다.
- 확정 후 생성된 Schedule이 월별 조회에서 정확히 한 번 나타난다.
- 중복 확정과 타 회원 접근이 차단된다.
- FastAPI 장애 중에도 Memo 생성 트랜잭션은 보존된다.
- AI 실패가 `FAILED` 상태로 조회 가능하다.

현재 판정:

```text
자동 테스트: PASS (47 tests, 0 failures)
HTTP 정상 Two-Phase E2E: PASS
중복 확정 방어: PASS
확정 Memo 삭제 방어: PASS
비일정/타 회원 가드레일: PENDING
HTTP 장애 E2E: PENDING
최종 판정: PARTIAL PASS
```

## 9. 현재 결론

정상 사용자 흐름에 필요한 핵심 계약은 실제 HTTP 환경에서 검증됐다.

```text
Memo 생성
→ PENDING
→ SUCCEEDED
→ 사용자 확정
→ CONFIRMED
→ Schedule 월별 조회 반영
```

동일 분석의 재확정과 확정된 원본 Memo 삭제도 각각 `MA003`, `MA004`로 차단됐다. 따라서 정상 Two-Phase 기능은 통과로 판정한다. 전체 E2E 최종 통과를 위해서는 FastAPI 종료 상태의 `FAILED` 전이와 비일정·타 회원 접근 테스트가 추가로 필요하다.

## 10. 후속 기록 규칙

수동 실험 완료 후 다음 정보를 이 문서에 추가한다.

- 실제 memoId와 scheduleId
- 각 요청의 HTTP 상태 및 핵심 응답 JSON
- `PENDING`, `SUCCEEDED`, `CONFIRMED`, `FAILED` 전이 여부
- Spring/FastAPI 핵심 로그 한 줄
- 예상값과 다른 동작 및 원인
- 최종 PASS/FAIL 판정

API 키, JWT 원문, 비밀번호 등 비밀정보는 기록하지 않는다.
