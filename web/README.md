# GHOST Web

[GHOST](../README.md) 모노레포의 **랜딩 페이지 · 지표 대시보드**.

- 담당: UX/UI / 프론트
- 상태: **스캐폴드. 구현 시작 전.**
- 규칙: [`AGENTS.md`](./AGENTS.md) — 작업 전 루트 [`AGENTS.md`](../AGENTS.md)도 읽으세요

## 배포

**Vercel 프로젝트 설정에서 Root Directory를 `web` 으로 지정해야 한다.** 모노레포이므로 이 설정이 없으면 빌드가 실패한다. 상세는 [`AGENTS.md`](./AGENTS.md).

## 범위

- 랜딩 페이지 (프로젝트 소개, 데모 영상)
- 지표 대시보드 — [`shared/events.md`](../shared/events.md)의 지표 정의를 따른다. **복귀율이 핵심 가설 검증 지표다.**

**API 키를 이 폴더에 넣지 않는다.** 브라우저 번들은 전부 공개된다. 서버 API를 경유한다.
