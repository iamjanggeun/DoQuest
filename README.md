# DoQuest (두퀘스트)

> **"취준생을 위한 AI 기반 할 일 RPG 서비스"**
> 대충 적은 일정 메모에서 AI가 마감일과 핵심 맥락을 자동으로 추출하고, 할 일을 완료할 때마다 내 도트 캐릭터가 성장하는 게이미피케이션(Gamification) To-Do 웹 애플리케이션입니다.

---

## 🛠️ Tech Stack & Architecture
- **Backend (Main):** Java 17 / Spring Boot 3.5.16 / Spring Data JPA
- **AI / LLM:** Python 3.10+ / FastAPI / LangChain / OpenAI API
- **Database:** TBD (Core) / Chroma (Vector DB)

---

## 🌟 핵심 기능 (MVP Scope)
1. **자연어 기반 일정 파싱 (AI):** 메모장에 자유롭게 적은 글을 분석하여 데드라인과 태스크 자동 등록
2. **퀘스트 & 캐릭터 성장 시스템:** 할 일 완료 시 캐릭터 경험치 획득 및 상태 변화