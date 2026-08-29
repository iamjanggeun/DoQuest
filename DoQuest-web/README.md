# DoQuest Web

DoQuest의 캘린더 및 AI 메모 Two-Phase 흐름을 검증하는 React + TypeScript MVP입니다.

- 캘린더와 메모장 단독 화면
- 입력 중단 후 메모 자동 저장
- 사용자가 `AI로 일정 찾기`를 실행한 경우에만 분석 시작
- 분석 중 자동 상태 확인 및 일정 후보 즉시 표시
- 사용자 확정 후 캘린더 등록

## 실행

Spring Boot 서버를 `localhost:8080`에서 먼저 실행합니다.

```bash
npm install
npm run dev
```

웹 화면은 `http://localhost:5173`에서 열립니다. Vite 개발 프록시가 `/api` 요청을 Spring Boot로 전달합니다.

AI 분석까지 확인하려면 FastAPI 서비스도 `localhost:8000`에서 실행해야 합니다.

## 검증

```bash
npm run lint
npm run build
```
