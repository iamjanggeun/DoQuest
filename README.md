# DoQuest (두퀘스트)

> **"취준생을 위한 AI 기반 할 일 RPG 서비스"**
> 대충 적은 일정 메모에서 AI가 마감일과 핵심 맥락을 자동으로 추출하고, 할 일을 완료할 때마다 내 도트 캐릭터가 성장하는 게이미피케이션(Gamification) To-Do 웹 애플리케이션입니다.
> 추후 취준생 뿐만이 아닌 학생, 수험생, 직장인 모두가 사용할 수 있도록 확장할 예정입니다.
---

## Tech Stack & Architecture
- **Backend (Main):** Java 17 / Spring Boot 3.5.16 / Spring Data JPA
- **AI / LLM:** Python 3.10+ / FastAPI / LangChain / OpenAI API
- **Database:** TBD (Core) / Chroma (Vector DB)

---

## 핵심 기능 (MVP Scope)
1. **자연어 기반 일정 파싱 (AI):** 메모장에 자유롭게 적은 글을 분석하여 데드라인과 태스크 자동 등록
2. **퀘스트 & 캐릭터 성장 시스템:** 할 일 완료 시 캐릭터 경험치 획득 및 상태 변화


# DoQuest : AI & Gamification 기반 일정 관리 시스템

## 1. 게이미피게이션 시스템 (Gamification Engine)
- **메커니즘:** '다마고치/썸원' 방식의 펫 육성 인터페이스 도입.
- **보상 루프 (Reward Loop):**
  - **Quest Completion:** 할 일 완료 시 '먹이(재화)' 및 경험치(EXP) 획득.
  - **Pet Interaction:** 먹이 주기를 통해 펫의 성장 레벨업 및 허기 상태 관리.
- **성장 공식 (Growth Algorithm):**
  - $\text{Next Level EXP} = \text{Current Level} \times 100$
  - 레벨 구간별 펫 진화 상태(`EGG` ➔ `HATCHED` ➔ `EVOLVED`) 전이 로직 적용.

## 2. 어뷰징 방지 및 검증 로직 (Abuse Prevention)
- **최소 수행 시간 검증 (Minimum Duration Validation):**
  - 퀘스트 생성 시간(`created_at`)과 완료 요청 시간(`completed_at`)의 차이를 검증.
  - 임계값(예: 퀘스트 등록 후 최소 5분) 미만 완료 시 `AbuseException` 예외 발생 및 경고 문구 출력.
  - **목적:** 무분별한 완료 버튼 연타를 통한 빠른 성장 편법 방지 및 데이터 무결성 확보.

## 3. 사용자 인터페이스 아키텍처 (UI/UX Architecture)
- **단일 통합 대시보드 (Single Integrated Dashboard):**
  - 화면 전환 비용을 줄이기 위해 **[펫 육성 / 캘린더 / 퀵 메모]**를 단일 화면에 모듈형 뷰로 배치.
  - 퀘스트 완료 이벤트 발생 시 WebSocket/SSE 또는 상태 변경을 통해 펫 애니메이션 즉시 동기화.

## 4. AI & RAG 파이프라인 (LangChain & Vector DB Integration)
- **자연어 파싱 (Structured Parsing):**
  - 자유 형식 메모 입력 시 LangChain `Structured Output`을 통해 [일정명, 마감일, 상세설명] JSON 추출.
- **비동기 정보 증강 (Asynchronous RAG):**
  - 일정 추출 후, 해당 키워드 기반 관련 정보/참고 링크를 Vector DB 및 외부 Search API로 조회.
  - 사용자 응답 속도 최적화를 위해 **일정 생성(동기)**과 **정보 증강(비동기)** 파이프라인을 분리 설계.

## 5. 캐릭터 확장성을 고려한 도메인 설계 (Extensible Domain Design)
- **개방-폐쇄 원칙(OCP) 적용:**
  - `Character`와 `User`를 1:1로 매핑하되, `CharacterType`과 진화 단계 스펙을 별도 도메인으로 분리.
  - 추후 신규 펫 캐릭터 추가 시 기존 비즈니스 로직 수정 없이 데이터/Enum 확장만으로 대응 가능하도록 설계.


## 엔티티 분석 다이어그램

![Entity Analysis Diagram](docs/images/entity-diagram.png)