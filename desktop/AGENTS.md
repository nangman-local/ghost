# desktop/ — Windows PC 클라이언트

Electron. 팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.
실행 방법과 트러블슈팅은 [`README.md`](./README.md).

담당: 정호(프론트, W3~)

> 이 폴더에 있던 `CLAUDE.md`는 독립 저장소 시절의 작업 현황 문서라 삭제했다.
> 제약·결정사항은 이 파일에, 실행·검증은 `README.md`에 있다. **중복해서 쓰지 않는다.**

**현재 상태:** Spike 4 완료. 활성 창 감지 + 투명 오버레이 캐릭터 + 클릭 통과, Windows 실기기 수동 검증 완료 (2026-09-14, 정호). 서버·Rive 연동은 미구현.

## 실행

- Electron 44.3 + get-windows 9.3, Node 24 이상
- ESM (`"type": "module"`), **preload만 `.cjs`**

```sh
npm install
npm start
```

npm 11+의 `allowScripts` 때문에 Electron 바이너리 다운로드가 막힐 수 있다. 안 되면 `node node_modules/electron/install.js`.

## 구조

```
src/main/main.js           앱 수명, IPC
src/main/activeWindow.js   활성 창 1초 폴링
src/main/overlay.js        투명·항상 위 창, 클릭 통과
src/main/platform/         OS별 분기 (win32 / darwin)
src/preload/preload.cjs    contextBridge
src/renderer/overlay.*     캐릭터 렌더링
```

OS 의존 코드는 `platform/`에만 둔다.

## 감지 제약 (중요)

- **Windows에서는 브라우저 URL을 얻을 수 없다.** get-windows의 `url`은 macOS 전용. PC 판단은 **앱 이름과 창 제목만** 사용한다.
- **창 제목이 계속 바뀌는 앱이 있다** (터미널 스피너 등). `shared/distract-rules.json`의 `minHoldSeconds` 이상 유지될 때만 판정한다.
- get-windows는 바인딩 로드 실패 시 **에러 없이 `undefined`를 반환한다.** 반드시 방어 코드를 둔다.
- 활성 창 폴링은 변경 시에만 출력하고 GHOST 자기 프로세스는 제외한다.

## 오버레이 규칙

- 투명 · 항상 위(`screen-saver` 레벨) · 포커스 없음
- **클릭 통과 기본값:** `setIgnoreMouseEvents(true, {forward:true})`. 커서가 캐릭터 위일 때만 IPC로 `false`.
- **topmost 가려짐 대응:** 다른 topmost 창(캡처 도구, PIP, 메신저 알림)이 나중에 올라오면 오버레이가 그 아래로 내려간다. `bringOverlayToTop()`을 1초마다 + 활성 창 변경 시 호출한다. 이 호출을 제거하지 않는다.
- 캐릭터 기본 투명도 70%, 누르는 동안과 놓은 뒤 1.5초는 100% (`SOLID_HOLD_MS`).

## 보안

`contextIsolation` · `sandbox` · `nodeIntegration: false` · CSP를 유지한다. **API 키를 이 폴더에 넣지 않는다.**

## 플랫폼 제약 (OS 수준, 앱에서 못 막음)

셸 UI(시작 메뉴, Alt+Tab, 알림 센터), 독점 전체화면 게임, UAC 보안 데스크톱에는 오버레이가 가려지는 것이 정상이다.

## 알게 된 것 / 결정 사항

- **Windows 우선 개발, 맥은 나중에 지원 (2026-09-14 결정).** OS마다 달라지는 값은 `platform/{win32,darwin}.js`에만 두고 공통 코드에 `process.platform` 분기를 넣지 않는다. `darwin.js`는 미검증 초안(`verified: false`)이라 실행 시 경고가 뜬다.
- **드래그에 `-webkit-app-region: drag`를 쓰지 않는다.** 클릭 통과와 충돌한다. 렌더러 내부 CSS 좌표로 이동시킨다.
- **오버레이를 전체 화면 크기로 만든 이유:** 나중에 개입 3단계(화면 일부 가리기)가 필요하기 때문이다.
- get-windows는 N-API 프리빌드(`napi-9-win32-unknown-x64`)가 포함되어 `@electron/rebuild`가 필요 없다. macOS 프리빌드도 들어 있다.
- 테스트용 앱을 `Start-Process -WindowStyle Hidden`으로 띄우면 첫 show 호출이 숨김으로 바뀐다. 쓰지 말 것.
- 맥 예상 차이: 화면 기록 권한 없으면 창 제목이 빈 문자열, 손쉬운 사용 권한 없으면 URL 없음. 전체화면 앱은 별도 Space라 `visibleOnFullScreen` 필요. 배포에는 코드서명·공증 필요.

## IPC 채널

| 채널 | 방향 | 내용 |
| --- | --- | --- |
| `overlay:set-interactive` | renderer → main | 커서가 캐릭터 위인지 |
| `app:quit` | renderer → main | 종료 |
| `active-window:changed` | main → renderer | `{appName, title, domain, path, processId, at}` |

`domain`은 **macOS에서만** 값이 있고 Windows는 항상 `null`이다.

## 검증

- GDI 스크린샷(`CopyFromScreen`/`BitBlt`)에는 **투명 오버레이가 찍히지 않는다.** 표시 확인은 `webContents.capturePage()`나 Win32 Z-order 조회(`GetTopWindow`/`GetWindow`)를 쓴다. 데모 녹화는 Windows 캡처 도구나 OBS(Windows Graphics Capture)를 쓴다.
- **확인 완료 (2026-09-14, 10~20분 실사용):** 클릭 통과, 캐릭터 클릭·드래그, 캡처 도구 사용 후 topmost 복구, 다른 창 최대화 시에도 표시, 메모장 타이핑 중 포커스 유지. Chrome·탐색기·캡처 도구·터미널·카카오톡 전환 감지. 크래시 없음.
- **미확인:** 전체화면 영상/게임, 배율 125%/150%, 다중 모니터, CPU 사용량, VS Code 전환 감지.

## 다음 할 일

1. 트레이 아이콘 (종료·일시정지·디버그 표시 토글)
2. 감지 이벤트 정규화 `{ appName, title, since, durationSec }` + 유지 시간 디바운스
3. `shared/distract-rules.json` 기반 로컬 규칙 판단 (FOCUS/DISTRACT/AMBIGUOUS)
4. 개입 상태머신 — Android와 같은 전이 규칙 ([`shared/states.md`](../shared/states.md))
5. 서버 연동: 30초 하트비트, WebSocket 이벤트 수신
6. Rive 캐릭터 연동 (`@rive-app/canvas`. 캔버스 하나로 그려지므로 클릭 판정은 경계 박스 또는 알파 샘플링)
7. 설정 창 · 할 일 선택 · "할 일 관련이야" 교정 UI
8. electron-builder로 Windows 설치 파일 만들기
9. (나중에) 맥 스파이크 — `darwin.js` 실기기 검증, 권한 온보딩, 코드서명·공증
