# CLAUDE.md

**이 저장소의 규칙은 [`AGENTS.md`](./AGENTS.md) 에 있습니다. 작업 시작 전에 반드시 읽으세요.**

작업할 폴더의 `AGENTS.md` 도 함께 읽습니다:

- [`android/AGENTS.md`](./android/AGENTS.md) — Kotlin/Compose, 권한, 서비스 규칙
- [`desktop/AGENTS.md`](./desktop/AGENTS.md) — Electron 규칙
- [`server/AGENTS.md`](./server/AGENTS.md) — 서버 규칙
- [`web/AGENTS.md`](./web/AGENTS.md) — Vercel Root Directory 지정 대상

공통 정의(상태 이름·API 스키마·규칙 사전·이벤트)는 [`shared/`](./shared/) 가 단일 진실의 원천입니다.

서비스 개요는 [`README.md`](./README.md), 브랜치·커밋·PR 규칙은 [`CONTRIBUTING.md`](./CONTRIBUTING.md) 에 있습니다.

## 작업 후

코드를 바꿨으면 **해당 폴더의 `AGENTS.md` 도 갱신하세요.** 새로 알게 된 제약이나 결정사항을 적습니다.
CI(`.github/workflows/agents-md-check.yml`)가 검사하며, 갱신이 없으면 PR이 실패합니다.
