# desktop/ — Windows PC 클라이언트

Electron. 팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.

담당: 정호(프론트, W3~)

**현재 상태:** Spike 4 완료. 활성 창 감지 + 투명 오버레이 캐릭터 + 클릭 통과, 실기기 수동 검증 완료. 서버 연동은 미구현. 작업 상세는 [`CLAUDE.md`](./CLAUDE.md) 참고.

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

## 검증

- GDI 스크린샷(`CopyFromScreen`/`BitBlt`)에는 **투명 오버레이가 찍히지 않는다.** 표시 확인은 `webContents.capturePage()`나 Win32 Z-order 조회(`GetTopWindow`/`GetWindow`)를 쓴다.
- 미확인 항목: 전체화면 영상/게임, 배율 125%/150%, 다중 모니터, CPU 사용량, VS Code 전환 감지
