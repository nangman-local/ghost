# CLAUDE.md — GHOST Desktop 작업 현황

다른 컴퓨터나 새 AI 세션에서 작업을 이어가기 위한 요약입니다. 실행 방법과 트러블슈팅은 `README.md`를 보세요.

## 프로젝트 맥락

- **GHOST**: 딴짓하는 순간 플로팅 캐릭터가 먼저 개입해 할 일로 복귀를 유도하는 집중 동반자 서비스
- 이 저장소(`nangman-local/desktop`)는 **Windows PC 클라이언트(Electron)** 입니다. Android, server, shared는 별도 저장소/폴더입니다.
- 팀 공통 규칙은 원래 모노레포 루트의 `AGENTS.md`에 있습니다 (이 저장소에는 포함되지 않음). 핵심은 아래와 같습니다.
  - 세션 상태 `WAITING | FOCUS | DISTRACT`, 개입 단계 `interventionLevel 0~3`, 활성 기기 `PC | ANDROID`
  - 서버 세션이 진실의 원천, 기기는 30초마다 하트비트, PC는 WebSocket
  - 딴짓 판단: MVP는 로컬 규칙 사전(`shared/rules-dictionary.json`) 우선, 모호한 경우만 서버 `/judge`. AI 없이도 동작해야 함
  - 오탐이 미탐보다 치명적이며, 모호하면 개입하지 말고 캐릭터가 물어봄. 짧은 행동은 누적하지 않음
  - **API 키는 서버에만 둔다.** 이 저장소에 LLM/외부 API 키를 절대 넣지 않는다
  - 서버로는 도메인과 제목만 보내고, URL 쿼리스트링과 화면 텍스트는 보내지 않는다
  - 크리티컬 패스(감지·오버레이·상태머신) 변경은 Android 코어 오너(정수) 확인 후 머지
- 담당: 정호(프론트). W1~2는 Android Compose 화면, W3~는 Electron PC 클라이언트
- 협업: 경민(서버·WebSocket·하트비트), 진형(/judge·규칙 사전), 보민(Rive 캐릭터), 호연(UI 디자인)

## 현재 상태 (2026-09-14 기준, 브랜치 `spike4`)

### 스파이크 목표
1. 활성 창의 앱 이름·창 제목 출력
2. 투명·항상 위 창에 임시 캐릭터 표시
3. 캐릭터 외 영역 클릭 통과

### 완료
- Electron 44.3 + get-windows 9.3 프로젝트 구성 (ESM, `"type": "module"`, preload만 `.cjs`)
- 활성 창 1초 폴링, 변경 시에만 출력, GHOST 자기 프로세스 제외 → **실행 확인 완료** (터미널→메모장 전환 감지)
- 주 모니터 작업 영역 전체를 덮는 투명·항상 위(`screen-saver` 레벨)·포커스 없음 오버레이
- 임시 CSS 유령 캐릭터 + 활성 창 말풍선. 렌더러가 정상적으로 그리는 것은 `capturePage`로 확인
- 클릭 통과: 기본 `setIgnoreMouseEvents(true, {forward:true})`, 커서가 캐릭터 위일 때만 IPC로 `false`
- 캐릭터 드래그(렌더러 내부 CSS 좌표 이동), 클릭 반응, 우클릭/`Ctrl+Shift+Q` 종료
- 보안 기본값: `contextIsolation`, `sandbox`, `nodeIntegration: false`, CSP
- **실기기 확인:** 화면에 캐릭터·말풍선 표시, Chrome/탐색기/캡처 도구/터미널 전환 감지 (`docs/images/` 스크린샷)
- **topmost 가려짐 수정:** 다른 topmost 창(캡처 도구, PIP, 메신저 알림 등)이 나중에 올라오면 오버레이가 그 아래로 내려가 복구되지 않던 문제. `bringOverlayToTop()`(`setAlwaysOnTop` + `moveTop`)을 1초마다, 그리고 활성 창 변경 시 호출해서 해결. Win32 Z-order 조회로 재현했고, 수정 후 1초 안에 복구되며 포커스도 유지됨을 확인

### 미확인 (실기기 수동 검증 필요)
README의 "수동 검증 체크리스트"를 참고하세요. 특히 아래 항목입니다.
- 캐릭터 외 영역 클릭이 뒤쪽 앱으로 통과하는지
- 실제 캡처 도구/PIP 사용 중 캐릭터가 다시 위로 올라오는지 (자동 테스트는 메모장을 topmost로 올려서 확인)
- 캐릭터가 떠 있어도 입력 포커스가 유지되는지
- 전체화면 앱, 배율 125%/150%, 다중 모니터

## 알게 된 것 / 결정 사항

- **Windows에서는 브라우저 URL을 얻을 수 없다** (get-windows의 `url`은 macOS 전용). PC 판단은 앱 이름과 창 제목만 사용 → 진형 님에게 공유 필요
- **창 제목이 계속 바뀌는 앱이 있다** (터미널 스피너 `◐/◑` 등). 판단 로직은 앱/제목이 N초(15~30초) 이상 유지될 때만 평가해야 함
- get-windows는 N-API 프리빌드(`napi-9-win32-unknown-x64`)가 포함되어 있어 `@electron/rebuild`가 필요 없음. 바인딩 로드에 실패하면 **에러 없이 `undefined`를 반환**함
- npm 11+의 `allowScripts` 때문에 Electron 바이너리 다운로드가 막힐 수 있음 → `package.json`에 허용 추가, 안 되면 `node node_modules/electron/install.js`
- GDI 스크린샷(`CopyFromScreen`/`BitBlt`)에는 투명 오버레이가 찍히지 않음. 표시 여부를 자동으로 확인하려면 `webContents.capturePage()`나 Win32 Z-order 조회(`GetTopWindow`/`GetWindow`)를 사용
- 셸 UI(시작 메뉴, Alt+Tab, 알림 센터), 독점 전체화면 게임, UAC 화면에는 가려지는 것이 정상 (OS 제약)
- 테스트용 앱을 `Start-Process -WindowStyle Hidden`으로 실행하면 첫 show 호출이 숨김으로 바뀌므로 쓰지 말 것
- 드래그에 `-webkit-app-region: drag`를 쓰지 않음 (클릭 통과와 충돌)
- 오버레이를 전체 화면으로 만든 이유: 나중에 3단계(화면 일부 가리기) 개입이 필요하기 때문

## 코드 지도

| 파일 | 역할 |
| --- | --- |
| `src/main/main.js` | 진입점. 오버레이 생성, 감지 시작, 종료 단축키, 단일 인스턴스 |
| `src/main/activeWindow.js` | `watchActiveWindow({ onChange })`: 폴링·중복 제거·자기 PID 제외 |
| `src/main/overlay.js` | `createOverlayWindow()`: 투명 창, `overlay:set-interactive` IPC, 디스플레이 변경 대응 |
| `src/preload/preload.cjs` | `window.ghost.{setInteractive, quit, onActiveWindowChanged}` |
| `src/renderer/overlay.{html,js}` | 캐릭터·말풍선 UI, 캐릭터 위 판정, 드래그 |
| `scripts/probe-active-window.js` | Electron 없이 get-windows만 테스트 (`npm run probe`) |

IPC 채널: `overlay:set-interactive`(renderer→main), `app:quit`(renderer→main), `active-window:changed`(main→renderer, `{appName, title, path, processId, at}`)

## 다음 할 일 (우선순위 순)

1. [ ] README 체크리스트로 실기기 수동 검증 → 결과를 이 파일에 반영
2. [ ] 트레이 아이콘 (종료·일시정지·디버그 표시 토글)
3. [ ] 감지 이벤트 정규화 `{ appName, title, since, durationSec }` + 유지 시간 디바운스
4. [ ] `shared/api-schema`, `shared/rules-dictionary.json` 형식을 서버 팀과 합의하고, 로컬 규칙 판단(FOCUS/DISTRACT/AMBIGUOUS) 구현
5. [ ] 개입 상태머신 (Android와 같은 전이 규칙 표로 합의)
6. [ ] 서버 연동: 30초 하트비트, WebSocket 이벤트 수신
7. [ ] Rive 캐릭터 연동 (`@rive-app/canvas`, 클릭 판정은 경계 박스 또는 알파 샘플링)
8. [ ] 설정 창·할 일 선택·"할 일 관련이야" 교정 UI (호연 님 디자인)
9. [ ] electron-builder로 Windows 설치 파일 만들기

## 개발 환경

- Node.js 24 이상 (`.nvmrc`, `engines`). 작성자 PC 기준 Node 24.18.0 / npm 11.16.0. npm 버전은 팀 합의가 없어 고정하지 않음
- 줄바꿈은 `.gitattributes`로 LF 통일 (Windows 스크립트 `.bat/.cmd/.ps1`만 CRLF)

## 브랜치

- 스파이크 작업 브랜치: `spike4` (`main`의 Initial commit에서 분기)
- 이어서 작업하려면: `git clone https://github.com/nangman-local/desktop.git` → `git checkout spike4` → `npm install` → `npm start`
