# CONTRIBUTING

이 문서는 GHOST 팀의 Git/GitHub 협업 규칙을 정의합니다. 개발 5명은 아래 규칙을 따릅니다.
디자인팀(2명)은 Git을 사용하지 않으며, 산출물은 별도 채널(피그마 등)로 공유 후 개발팀이 반영합니다.

프로젝트 규칙(아키텍처·상태 정의·절대 규칙)은 [`AGENTS.md`](./AGENTS.md)에 있습니다. **이 문서는 협업 절차만 다룹니다.**

---

## 0. 작업 시작 전 (필수)

프로젝트가 처음이라면 [`README.md`](./README.md)로 서비스 개요를 먼저 파악하세요.

1. **루트 [`AGENTS.md`](./AGENTS.md)를 읽는다** — 팀 공통 헌법
2. **작업할 폴더의 `AGENTS.md`를 읽는다** — `android/` · `desktop/` · `server/` · `web/`
3. 상태 이름이나 API를 쓴다면 [`shared/`](./shared/)를 확인한다

AI 코딩 도구(Claude Code, Codex 등)를 쓴다면 `AGENTS.md`를 컨텍스트에 넣고 시작하세요.
Claude Code는 [`CLAUDE.md`](./CLAUDE.md)를 자동으로 읽어 `AGENTS.md`로 안내합니다.

---

## 1. 브랜치 전략 (Git Flow 간소화)

| 브랜치 | 역할 | 비고 |
|---|---|---|
| `main` | 배포/데모용 안정 버전 | 직접 push 금지, PR만 허용 (브랜치 보호 설정) |
| `develop` | 개발 통합 브랜치 | 평소 작업은 여기로 머지됨 |
| `feature/*` | 기능 개발 | `develop`에서 분기, `develop`으로 머지 |
| `fix/*` | 버그 수정 | `develop`에서 분기, `develop`으로 머지 |
| `infra/*` | 인프라/CI·CD 설정 | `develop`에서 분기, `develop`으로 머지 |
| `hotfix/*` | 배포 후 긴급 수정 | `main`에서 분기, `main`과 `develop` 양쪽에 머지 |

### 흐름

1. 새 작업 시작 → `develop`에서 브랜치 생성
2. 작업 완료 → `develop`으로 PR 생성 → 리뷰 후 머지
3. 중간 발표/최종 데모 등 **배포가 필요한 시점**에 `develop` → `main`으로 PR 생성 후 머지
4. 배포 후 급한 버그 발견 시에만 `hotfix/*`를 `main`에서 분기

### 브랜치 이름 규칙

```
타입/이슈번호-짧은설명(영문, 소문자, 하이픈)
```

예시:

```
feature/14-android-heartbeat
fix/8-vscode-detection
infra/12-ci-pipeline-setup
```

---

## 2. 커밋 메시지 컨벤션 (Conventional Commits + 이슈번호)

```
<타입>: <설명> (#이슈번호)
```

### 타입 목록

| 타입 | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (README, AGENTS.md, API 명세 등) |
| `style` | 코드 포맷팅, 세미콜론 등 (로직 변경 없음) |
| `refactor` | 코드 리팩토링 (기능 변화 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 패키지 매니저, 설정 파일 등 기타 변경 |
| `infra` | CI/CD, 배포, 서버 인프라 설정 |
| `design` | 디자인 자산 반영 (이미지, 스타일 등) |

### 예시

```
feat: 30초 하트비트 전송 구현 (#14)
fix: topmost 가려짐 복구 누락 수정 (#8)
docs: AGENTS.md에 Chrome 감지 제약 추가 (#2)
infra: GitHub Actions 배포 파이프라인 구성 (#12)
design: 유령 캐릭터 IDLE 모션 반영 (#16)
```

### 작성 규칙

- 제목은 50자 이내, 끝에 마침표 없음
- 한 커밋 = 한 가지 작업 단위로 최대한 쪼개기
- 본문(선택)이 필요하면 제목 아래 한 줄 띄우고 상세 내용 작성

---

## 3. 이슈(Issue) 관리

- 작업 시작 전 **GitHub Issue를 먼저 생성**하고, 커밋/브랜치명에 이슈번호를 포함시킵니다.
- 이슈 제목: `[Feature] 30초 하트비트 구현`, `[Bug] VS Code 전환 미감지`, `[Infra] CI 파이프라인 구성`
- **담당자(Assignee)와 라벨 2종을 지정합니다.**

### 라벨 체계

두 축으로 관리합니다.

| 축 | 라벨 |
|---|---|
| **타입** | `feat` · `fix` · `docs` · `infra` · `design` |
| **영역** | `android` · `desktop` · `server` · `web` · `shared` |
| **상태(선택)** | `mvp`(MVP 필수) · `blocked`(다른 작업에 의존) · `verification`(실기기 검증 필요) |

---

## 4. PR(Pull Request) 규칙

- PR 제목은 커밋 컨벤션과 동일하게: `feat: 30초 하트비트 전송 구현 (#14)`
- **본인이 아닌 팀원 1명 이상의 승인(Approve)** 후 머지 가능
- 머지 방식: **Squash and Merge** (커밋 히스토리 정리)
- 머지 후 원본 브랜치는 삭제
- PR 템플릿의 체크리스트를 모두 채웁니다

### 크리티컬 패스

**감지·오버레이·상태머신 변경은 Android 코어 오너(정수) 리뷰 후** 머지합니다.

---

## 5. 작업 후 — AGENTS.md 갱신 (필수)

**코드를 바꿨으면 해당 폴더의 `AGENTS.md`도 갱신합니다.** CI가 검사하며, 코드만 바꾸고 문서를 안 고치면 PR이 실패합니다.

### 무엇을 적나

- 새로 알게 된 플랫폼 제약 (예: "Windows에서는 브라우저 URL을 얻을 수 없다")
- 우회 방법과 그 이유 (예: "topmost 복구를 위해 1초마다 `bringOverlayToTop()` 호출")
- 하면 안 되는 것 (예: "드래그에 `-webkit-app-region: drag`를 쓰지 않는다")
- 바뀐 구조·파일 역할

### 무엇을 안 적나

- 코드를 읽으면 바로 보이는 것
- 이번 PR에서만 유효한 임시 정보

### CI가 검사하지 않는 변경

문서(`.md`), 이미지, 잠금 파일(`package-lock.json`, `.gitignore`, `.gitattributes`, `.nvmrc`)**만** 바꾼 PR은 `AGENTS.md` 갱신을 요구하지 않습니다. README 오타를 고칠 때마다 CI가 실패하면 면제 사유를 습관적으로 쓰게 되고, 그러면 검사 자체가 무의미해지기 때문입니다.

### 면제

그 외에 문서 변경이 정말 불필요하면 PR 본문에 **"문서 변경이 불필요한 이유"**를 적으면 CI를 통과합니다.

`shared/`를 바꿨다면 **팀 공유 채널에 공지하고** 세 클라이언트를 동시에 갱신합니다.

---

## 6. 완료 기준

**실기기에서 확인한 것만 완료입니다.** 에뮬레이터 동작은 완료가 아니며, Android는 삼성 기기 최소 1대 확인이 필요합니다.

---

## 7. 디자인팀 협업 방식

디자인팀(2명)은 Git을 사용하지 않습니다.

- 디자인 산출물은 피그마 등 별도 툴에서 공유
- 개발팀이 디자인 자산(이미지, 아이콘, 스타일 가이드 등)을 반영할 때 `design:` 타입 커밋 사용

  ```
  design: 유령 캐릭터 IDLE 모션 반영 (#16)
  ```

- 디자인 요청/피드백은 GitHub Issue에 `design` 라벨을 달아 기록 (선택 사항이지만 이력 관리에 도움됨)
- Rive 캐릭터 `.riv`는 Android·PC 공용 파일 1개입니다. 상태 이름은 [`shared/states.md`](./shared/states.md)의 `characterState`와 일치해야 합니다

---

## 8. 브랜치 보호 규칙 (인프라 담당자 설정)

현재 적용된 설정:

- **`main`** — 직접 push 금지, PR 필수, 리뷰 1명 승인 필수, `AGENTS.md 갱신 여부` CI 통과 필수
- **`develop`** — 보호 없음 (초기 개발 속도 우선). 팀 합의로 언제든 `main`과 같은 수준으로 올릴 수 있습니다

---

## 9. 기타

- 기술 스택 확정 후 이 문서에 **실행 방법, 로컬 개발 환경 세팅** 섹션을 추가 예정
  - 현재 실행 방법은 [`android/README.md`](./android/README.md), [`desktop/README.md`](./desktop/README.md) 참고
- 규칙은 팀 회의를 통해 필요 시 개정 가능
