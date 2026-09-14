# web/ — 랜딩·대시보드

팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.

담당: 호연(UX/UI) · 정호(프론트)

**현재 상태:** 스캐폴드. 구현 시작 전.

## Vercel 배포

**이 폴더가 Vercel Root Directory 지정 대상이다.**

Vercel 프로젝트 설정에서 **Root Directory를 `web`으로 지정한다.** 모노레포이므로 이 설정이 없으면 빌드가 실패한다.

- Build Command / Output Directory는 프레임워크 기본값을 쓴다
- `web/` 밖의 파일을 빌드 입력으로 참조하지 않는다. `shared/`가 필요하면 값을 복사하거나 빌드 스크립트로 가져온다 (Vercel 빌드 컨텍스트는 Root Directory 기준이다)

## 범위

- 랜딩 페이지 (프로젝트 소개, 데모 영상)
- 지표 대시보드 — [`shared/events.md`](../shared/events.md)의 지표 정의를 따른다. 복귀율이 핵심 가설 검증 지표다.

## 규칙

- **API 키를 이 폴더에 넣지 않는다.** 브라우저 번들은 전부 공개된다. 서버 API를 경유한다.
- API 응답 타입은 [`shared/api-schema.md`](../shared/api-schema.md)를 따른다.
- 상태 이름은 [`shared/states.md`](../shared/states.md)의 문자열을 그대로 쓴다.
