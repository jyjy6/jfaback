# Job Flow Agent (JFA)

이 프로젝트는 **사용자가 등록한 이력서/문서와 채용공고를 기반으로**, `LangChain4j + RAG`를 활용해 맞춤 취업 코칭, 제안, 분석, 평가 조언을 제공하는 애플리케이션입니다.

- 프론트엔드: `Nuxt 3` + `Vue 3` + `TypeScript` + `Pinia` + `Vuetify`
- 백엔드: `Spring Boot` + `Java 21` + `LangChain4j` + `MySQL` + `Redis` + `Pinecone`
- AI 모델:
  - 채팅 스트리밍: OpenAI (`gpt-4o-mini`)
  - 구조화/분석/임베딩: Google Gemini (`gemini-2.5-pro`, `gemini-embedding-001`)

---

## 핵심 기능

- **이력서/문서 업로드 및 RAG 인덱싱**
  - 사용자가 업로드한 문서를 청크 분할 후 임베딩하여 벡터 스토어(Pinecone)에 저장
  - 사용자별 메타데이터(`username`) 필터링으로 개인화 검색
  - 이미지: <img src="https://placehold.co/1200x600/png?text=README+Image+Test" alt="업로드 화면 + 청크/임베딩 처리 흐름도" width="800" />
  - 설명(예정): 어떤 형식의 문서를 어떻게 벡터화하는지 요약
- **AI 취업 코칭 대화**
  - `/api/v1/ai/chat` SSE 스트리밍 응답
  - 부분 토큰을 실시간 렌더링하고, 필요 시 Tool 호출 결과를 UI 이벤트로 전송
  - 이미지: <img src="https://placehold.co/1200x600/png?text=README+Image+Test" alt="채팅 화면 + message/ui_render 이벤트 예시 캡처" width="800" />
  - 설명(예정): 스트리밍 응답과 UI 카드 렌더링 동작 방식
- **채용공고 URL 분석**
  - Jsoup 기반 스크래핑 후 AI 구조화 분석
  - 회사명/기술스택/자격요건/우대사항 등 추출
  - 이미지: <img src="https://placehold.co/1200x600/png?text=README+Image+Test" alt="채용공고 링크 입력 전/후 비교 화면" width="800" />
  - 설명(예정): 스크래핑 텍스트가 구조화 데이터로 변환되는 과정
- **문서 관리**
  - 내 문서 목록 조회, 검색, 삭제
  - 삭제 시 DB 메타데이터 + Pinecone 벡터 삭제 처리
  - 이미지: <img src="https://placehold.co/1200x600/png?text=README+Image+Test" alt="문서 목록/삭제 확인 다이얼로그/삭제 결과 화면" width="800" />
  - 설명(예정): 문서 생명주기(업로드 → 조회 → 삭제) 요약

---

## 프로젝트 구조

```text
jfaproject/
├─ jfafront/        # Nuxt 3 프론트엔드
└─ JobFlowAgent/    # Spring Boot 백엔드 API + AI/RAG
```

### 프론트엔드 주요 화면 (`jfafront`)

- `pages/index.vue`: 랜딩
- `pages/login/index.vue`: 로그인/게스트 로그인
- `pages/resume/upload.vue`: 이력서(문서) 업로드
- `pages/dashboard.vue`: 내 문서 목록/삭제
- `components/AdminChatAgent.vue`: 스트리밍 채팅 UI + UI 이벤트 렌더링

### 백엔드 주요 모듈 (`JobFlowAgent`)

- `AI/Controller/AIController.java`: SSE 기반 AI 채팅
- `AI/RAG/Controller/RagController.java`: 문서 업로드/검색/목록/삭제
- `AI/RAG/Service/RagService.java`: 청크 분할, 임베딩, Pinecone 저장/검색
- `AI/Tools/*`: LangChain4j Tool (RAG 검색, 채용공고 스크래핑, UI 렌더링 등)
- `Config/LangChainConfig.java`: AI 모델/임베딩/스토어/메모리 설정

---

## 동작 흐름

1. 사용자가 문서(이력서 등)를 업로드
2. 백엔드가 문서를 파싱/청크 분할 후 임베딩 생성
3. 임베딩 + 메타데이터를 Pinecone에 저장
4. 사용자가 AI에 질문
5. AI가 필요 시 Tool 호출:
   - 사용자 문서 검색(RAG)
   - 채용공고 URL 분석
   - UI 카드 렌더링 이벤트 발행
6. 프론트가 SSE 스트림(`message`, `ui_render`)을 받아 실시간 출력

---

## 사전 요구사항

- Node.js 20+
- Java 21
- MySQL 8+
- Redis
- Pinecone 인덱스
- Google Gemini API Key
- OpenAI API Key

---

## 환경 변수 및 설정

### 1) 프론트엔드 (`jfafront/.env.development`)

```env
NUXT_PUBLIC_API_BASE=http://localhost:8080/api/v1
NUXT_API_BASE_SERVER=http://localhost:8080/api/v1
NUXT_PUBLIC_SITE_URL=http://localhost:3000
```

### 2) 백엔드 (`JobFlowAgent/src/main/resources/application-dev.properties`)

실행에 필요한 대표 설정:

- DB: `spring.datasource.*`
- Redis: `spring.data.redis.*`
- Gemini: `google.gemini.api.key`
- OpenAI: `openai.api.key`
- Pinecone: `pinecone.api.key`, `pinecone.index.name`, `pinecone.namespace`
- JWT 키: `jwt.private-key`, `jwt.public-key`

> 권장: 민감정보(API 키/DB 비밀번호)는 저장소에 직접 커밋하지 말고, 로컬 환경변수 또는 별도 비공개 설정 파일로 관리하세요.

---

## 실행 방법

### 1) 백엔드 실행

```bash
cd JobFlowAgent
./gradlew bootRun
```

- 기본 포트: `8080`
- 활성 프로파일: `dev` (`application.properties` 기준)

### 2) 프론트엔드 실행

```bash
cd jfafront
npm install
npm run dev
```

- 기본 포트: `3000`

---

## 주요 API

기본 prefix: `/api/v1`

### 인증

- `POST /auth/login`
- `POST /auth/login/guest`
- `GET /auth/refresh-token`
- `POST /auth/logout`

### 회원

- `POST /member/register`
- `GET /member/userinfo`

### RAG 문서

- `POST /rag/ingest` (multipart file)
- `POST /rag/ingest/text`
- `POST /rag/search`
- `GET /rag/documents`
- `GET /rag/documents/my`
- `GET /rag/documents/{id}`
- `DELETE /rag/documents/delete/{id}`

### AI 채팅

- `POST /ai/chat` (SSE stream)
  - 이벤트: `message`, `ui_render`, `error`

---

## 개발 참고

- API 문서(Springdoc): `http://localhost:8080/swagger-ui/index.html`
- CORS 설정: 현재 `allowedOriginPatterns("*")`로 개방되어 있으므로 운영 환경에서는 제한을 권장
- 채팅 메모리: Redis 기반 `MessageWindowChatMemory` 사용

---

## 향후 개선 아이디어

- 환경별 설정 분리(`application-local.properties` + `.env` 표준화)
- 문서 업로드/임베딩 파이프라인 비동기 큐 처리
- 테스트 코드 보강(통합 테스트, Tool 호출 시나리오)
- 운영 보안 강화(CORS 제한, 비밀키 로테이션)
