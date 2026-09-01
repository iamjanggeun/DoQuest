# DoQuest API JSON Reference

이 문서는 현재 Spring Boot API와 Spring-FastAPI 내부 계약의 요청·응답 JSON 기준입니다. 구현 코드의 DTO와 컨트롤러를 기준으로 작성했으며, 기능 변경 시 코드와 함께 갱신합니다.

## 공통 규칙

- Spring Base URL: `http://localhost:8080`
- FastAPI Base URL: `http://localhost:8000`
- Content-Type: `application/json`
- 인증 필요 API: `Authorization: Bearer <access-token>`
- 날짜: `YYYY-MM-DD`
- 시간: 24시간제 `HH:mm`; 시간이 없는 일정은 `null`
- 본문이 없는 성공 응답은 `(no body)`로 표시

### 공통 오류 응답

```json
{
  "code": "C001",
  "message": "적절하지 않은 요청 값입니다."
}
```

주요 오류 코드는 [오류 코드](#error-codes)에서 확인할 수 있습니다.

## Auth

<a id="auth-signup"></a>
### 회원가입

`POST /api/v1/auth/signup` → `201 Created`

```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "도퀘스터",
  "petName": "두부"
}
```

응답은 생성된 회원 ID입니다.

```json
1
```

<a id="auth-login"></a>
### 로그인

`POST /api/v1/auth/login` → `200 OK`

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

## Dashboard

<a id="dashboard-get"></a>
### 대시보드 조회

`GET /api/v1/dashboard` → `200 OK`

```json
{
  "pet": {
    "id": 1,
    "name": "두부",
    "level": 3,
    "exp": 120,
    "stage": "성장기"
  },
  "quests": [
    {
      "id": 10,
      "title": "알고리즘 문제 풀이",
      "category": "학습",
      "status": "IN_PROGRESS",
      "rewardExp": 20
    }
  ]
}
```

## Memo

<a id="memo-create"></a>
### 메모 생성

메모만 저장하며 AI 분석은 자동으로 시작하지 않습니다.

`POST /api/v1/memos` → `201 Created`

```json
{
  "content": "오늘 오후 7시 강남역에서 스터디"
}
```

응답은 생성된 Memo ID입니다.

```json
12
```

<a id="memo-list"></a>
### 메모 목록 조회

`GET /api/v1/memos` → `200 OK`

```json
[
  {
    "id": 12,
    "content": "오늘 오후 7시 강남역에서 스터디",
    "isParsed": true,
    "createdAt": "2026-08-31T16:20:10.123456"
  }
]
```

<a id="memo-update"></a>
### 메모 수정

`PATCH /api/v1/memos/{memoId}` → `200 OK`

```json
{
  "content": "오늘 오후 8시 강남역에서 스터디"
}
```

응답: `(no body)`

분석 중인 메모는 수정할 수 없습니다. 확정 전 분석 결과가 존재하면 결과를 삭제하고 `isParsed=false`로 되돌립니다.

<a id="memo-delete"></a>
### 메모 삭제

`DELETE /api/v1/memos/{memoId}` → `204 No Content`

응답: `(no body)`

확정된 Schedule과 연결된 메모는 삭제할 수 없습니다.

## Memo Analysis

<a id="analysis-request"></a>
### AI 분석 시작

`POST /api/v1/memos/{memoId}/analysis` → `202 Accepted`

요청 본문: `(no body)`

기존 분석이 `FAILED`이면 실패 결과를 초기화하고 다시 분석합니다. 일시적 네트워크 오류, FastAPI `5xx`, `429 Too Many Requests`는 서버 내부에서 최대 3회 지수 백오프로 재시도합니다.

```json
{
  "memoId": 12,
  "status": "PENDING",
  "isSchedule": false,
  "title": null,
  "scheduledAt": null,
  "scheduledTime": null,
  "location": null,
  "summaryInfo": null
}
```

<a id="analysis-get"></a>
### AI 분석 결과 조회

`GET /api/v1/memos/{memoId}/analysis` → `200 OK`

```json
{
  "memoId": 12,
  "status": "SUCCEEDED",
  "isSchedule": true,
  "title": "강남역 스터디",
  "scheduledAt": "2026-08-31",
  "scheduledTime": "19:00",
  "location": "강남역",
  "summaryInfo": "스터디 모임 참석"
}
```

상태 값은 `PENDING`, `SUCCEEDED`, `FAILED`, `CONFIRMED`입니다.

<a id="analysis-confirm"></a>
### AI 분석 결과를 일정으로 확정

`POST /api/v1/memos/{memoId}/analysis/confirm` → `201 Created`

요청 본문: `(no body)`

```json
{
  "scheduleId": 31,
  "memoId": 12,
  "title": "강남역 스터디",
  "scheduledAt": "2026-08-31",
  "scheduledTime": "19:00",
  "location": "강남역",
  "summaryInfo": "스터디 모임 참석",
  "isCompleted": false
}
```

## Schedule

<a id="schedule-create"></a>
### 일정 생성

`POST /api/v1/schedules` → `201 Created`

수동 생성 시 `memoId`는 `null`입니다.
동일한 `memoId`로 이미 생성된 일정이 있으면 `409 Conflict / S004`를 반환합니다.

```json
{
  "memoId": null,
  "title": "운동",
  "scheduledAt": "2026-09-01",
  "scheduledTime": "18:30",
  "location": "헬스장",
  "summaryInfo": "하체 운동"
}
```

```json
{
  "scheduleId": 32,
  "memoId": null,
  "title": "운동",
  "scheduledAt": "2026-09-01",
  "scheduledTime": "18:30",
  "location": "헬스장",
  "summaryInfo": "하체 운동",
  "isCompleted": false
}
```

<a id="schedule-monthly"></a>
### 월별 일정 조회

`GET /api/v1/schedules?year=2026&month=9` → `200 OK`

```json
[
  {
    "scheduleId": 32,
    "memoId": null,
    "title": "운동",
    "scheduledAt": "2026-09-01",
    "scheduledTime": "18:30",
    "location": "헬스장",
    "summaryInfo": "하체 운동",
    "isCompleted": false
  }
]
```

<a id="schedule-get"></a>
### 일정 단건 조회

`GET /api/v1/schedules/{scheduleId}` → `200 OK`

```json
{
  "scheduleId": 32,
  "memoId": null,
  "title": "운동",
  "scheduledAt": "2026-09-01",
  "scheduledTime": "18:30",
  "location": "헬스장",
  "summaryInfo": "하체 운동",
  "isCompleted": false
}
```

<a id="schedule-update"></a>
### 일정 수정

`PATCH /api/v1/schedules/{scheduleId}` → `200 OK`

```json
{
  "title": "저녁 운동",
  "scheduledAt": "2026-09-01",
  "scheduledTime": "19:00",
  "location": "헬스장",
  "summaryInfo": "하체와 코어 운동"
}
```

응답은 수정된 [Schedule JSON](#schedule-get)과 같습니다.

<a id="schedule-completion"></a>
### 일정 완료 상태 변경

`PATCH /api/v1/schedules/{scheduleId}/completion` → `200 OK`

```json
{
  "completed": true
}
```

응답은 변경된 [Schedule JSON](#schedule-get)과 같습니다.

<a id="schedule-curations"></a>
### D-3 미완료 일정 조회

`GET /api/v1/schedules/curations` → `200 OK`

응답은 오늘부터 3일 이내인 미완료 Schedule 배열입니다.

```json
[
  {
    "scheduleId": 32,
    "memoId": null,
    "title": "운동",
    "scheduledAt": "2026-09-01",
    "scheduledTime": "19:00",
    "location": "헬스장",
    "summaryInfo": "하체와 코어 운동",
    "isCompleted": false
  }
]
```

<a id="schedule-delete"></a>
### 일정 삭제

`DELETE /api/v1/schedules/{scheduleId}` → `204 No Content`

응답: `(no body)`

## Quest

<a id="quest-create"></a>
### 퀘스트 생성

`POST /api/v1/quests` → `201 Created`

```json
{
  "title": "알고리즘 문제 풀이",
  "category": "STUDY"
}
```

응답: `(no body)`

카테고리 값은 `STUDY`, `EXERCISE`, `HEALTH`, `LIFESTYLE`입니다.

## Spring-FastAPI 내부 계약

브라우저가 직접 호출하는 API가 아니라 Spring의 비동기 AI 작업자가 호출하는 내부 계약입니다.

<a id="ai-parse-memo"></a>
### 메모 일정 분석

`POST http://localhost:8000/api/v1/ai/parse-memo` → `200 OK`

```json
{
  "memo_id": 12,
  "member_id": 1,
  "content": "오늘 오후 7시 강남역에서 스터디"
}
```

```json
{
  "is_schedule": true,
  "title": "강남역 스터디",
  "scheduled_at": "2026-08-31",
  "scheduled_time": "19:00",
  "location": "강남역",
  "summary_info": "스터디 모임 참석",
  "action_links": []
}
```

`action_links`는 RAG 미구현 단계에서 항상 빈 배열입니다.

<a id="error-codes"></a>
## 오류 코드

| HTTP | Code | 의미 |
|---:|---|---|
| 400 | `C001` | 요청 값 또는 형식 오류 |
| 500 | `C002` | 서버 내부 오류 |
| 404 | `M001` | 회원을 찾을 수 없음 |
| 409 | `M002` | 이메일 중복 |
| 401 | `A001` | 로그인 정보 불일치 |
| 404 | `MA001` | 메모 분석 결과 없음 |
| 409 | `MA002` | 확정 가능한 일정 분석 결과가 아님 |
| 409 | `MA003` | 이미 확정된 분석 결과 |
| 409 | `MA004` | 확정 일정과 연결된 메모 삭제 시도 |
| 409 | `MA005` | AI 분석 중 메모 수정 시도 |
| 404 | `S001` | 일정을 찾을 수 없음 |
| 400 | `S002` | 일정 날짜 형식 오류 |
| 400 | `S003` | 일정 시간 형식 오류 |
| 409 | `S004` | 동일한 메모로 생성된 일정이 이미 존재함 |
| 404 | `Q001` | 퀘스트를 찾을 수 없음 |
| 400 | `Q002` | 이미 완료된 퀘스트 |
| 409 | `Q003` | 유사 퀘스트 중복 |
| 400 | `Q004` | 진행 중이 아닌 퀘스트 완료 시도 |
| 400 | `Q005` | 생성 후 30분 이전 완료 시도 |
| 404 | `P001` | 회원의 펫을 찾을 수 없음 |

## 문서 유지 규칙

- DTO 필드 추가·삭제·이름 변경 시 해당 JSON 예시를 함께 수정합니다.
- 상태 코드나 오류 코드를 변경하면 README API 요약과 이 문서를 함께 수정합니다.
- Vector DB/RAG API가 구현되면 내부 계약 섹션에 추가합니다.
- 운영 DB 선정은 HTTP JSON 계약을 바꾸지 않지만 날짜·시간 타입의 실제 DB 호환성은 별도로 재검증합니다.
