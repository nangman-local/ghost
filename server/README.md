# GHOST Server

[GHOST](../README.md) 모노레포의 **백엔드**. Spring Boot + PostgreSQL (또는 Supabase).

- 담당: 서버·동기화 / AI 판단
- 상태: **스캐폴드. 구현 시작 전.**
- 규칙: [`AGENTS.md`](./AGENTS.md) — 작업 전 루트 [`AGENTS.md`](../AGENTS.md)도 읽으세요

## 책임

**서버 세션이 진실의 원천이다.** 현재 할 일, 상태, 활성 기기, 마지막 하트비트를 서버가 관리한다.

- 인증 · 할 일 · 세션 CRUD
- 하트비트 수신 (30초 주기) — 끊긴 기기는 비활성 처리
- `POST /judge` — 규칙 사전 + LLM 판단 + 캐시
- 이벤트 로그 수집, 지표 집계
- WebSocket 푸시 (PC 클라이언트용)
- Notion OAuth (조건부)

## API 계약

**[`shared/api-schema.md`](../shared/api-schema.md) 가 계약이다.** 상태 이름은 [`shared/states.md`](../shared/states.md), 이벤트는 [`shared/events.md`](../shared/events.md)를 따른다.

여기를 바꾸면 팀 공유 채널에 공지하고 세 클라이언트를 동시에 갱신한다.

## 실행

> 스택 확정 후 작성 예정. 시크릿은 환경변수로 주입하며 저장소에 커밋하지 않는다.

**LLM API 키는 이 서버에만 둔다.** 클라이언트로 내려보내지 않는다.
