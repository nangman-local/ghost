# server/ — 백엔드

Spring Boot + PostgreSQL (또는 Supabase). 팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.

담당: 서버·동기화 · AI 판단

**현재 상태:** 스캐폴드. 구현 시작 전.

## 이 서버의 책임

**서버 세션이 진실의 원천이다.** 현재 할 일, 상태, 활성 기기, 마지막 하트비트를 서버가 관리한다.

- 인증, 할 일, 세션 CRUD
- 하트비트 수신 (30초 주기) — 끊긴 기기는 비활성 처리
- `POST /judge` — 규칙 사전 + LLM 판단 + 캐시
- 이벤트 로그 수집, 지표 집계
- WebSocket 푸시 (PC 클라이언트용)
- Notion OAuth (조건부)

## API

**[`shared/api-schema.md`](../shared/api-schema.md)가 계약이다.** 여기를 바꾸면 팀 공유 채널에 공지하고 세 클라이언트를 동시에 갱신한다.

상태 이름은 [`shared/states.md`](../shared/states.md), 이벤트는 [`shared/events.md`](../shared/events.md)를 따른다.

## `/judge` 구현 원칙

1. 서버도 [`shared/distract-rules.json`](../shared/distract-rules.json)을 먼저 적용한다. 규칙으로 끝나면 LLM을 호출하지 않는다.
2. LLM 호출 결과는 `(taskProfile, app, domain, title)` 키로 캐시한다. 응답에 `cached`를 실어 보낸다.
3. **AI가 실패해도 200으로 `AMBIGUOUS`를 반환한다.** 클라이언트가 개입하지 않도록 하는 것이 실패 모드다. 5xx로 클라이언트를 막지 않는다.
4. `confidence`를 반드시 채운다. 개입 3단계는 이 값에 의존한다.
5. 할 일 프로필 생성은 **할 일 등록 시 1회**만. 판단 요청마다 호출하지 않는다.

MVP는 규칙만으로 동작해야 한다. AI는 나중에 얹는다.

## 보안·프라이버시

- **LLM API 키는 이 서버에만 둔다.** 클라이언트로 내려보내지 않는다.
- 클라이언트에서 **도메인과 제목만** 받는다. URL 쿼리스트링·화면 텍스트를 받거나 저장하지 않는다.
- 이벤트 `payload`에 개인 식별 정보를 저장하지 않는다.
- 시크릿은 환경변수로 주입한다. 저장소에 커밋하지 않는다.

## 데이터 모델

처음부터 **다중 기기 동기화 전제**로 설계한다. 세션-기기는 1:N이며 각 기기의 `lastHeartbeat`를 따로 관리한다.
