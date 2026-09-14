# GHOST Desktop (Electron)

Windows PC 클라이언트. 현재는 **기술 스파이크** 단계입니다.

- 담당: 정호 (프론트)
- 상태: 스파이크 코드 작성 완료, 실기기 수동 검증 필요

## 스파이크 목표

1. 활성 창의 앱 이름·창 제목 출력
2. 투명·항상 위 창에 임시 캐릭터 표시
3. 캐릭터 외 영역은 클릭 통과

## 실행

```powershell
npm install
npm start
```

- 활성 창이 바뀌면 터미널에 `[active] 앱이름 | 창제목`이 출력되고, 캐릭터 옆 말풍선에도 표시됩니다.
- 종료: `Ctrl+Shift+Q` 또는 캐릭터 우클릭 (오버레이는 포커스를 받지 않기 때문에 창 닫기 버튼이 없음)
- get-windows만 따로 확인: `npm run probe` (3초 뒤 활성 창 1회 출력)

### 설치 시 주의 (npm 11+)

npm의 `allowScripts` 정책 때문에 install 스크립트가 기본으로 막힙니다.

- `electron`: 바이너리를 다운로드하는 스크립트가 필요합니다. `package.json`의 `allowScripts`에 허용해 두었습니다. 그래도 `node_modules/electron/dist/electron.exe`가 없으면 `node node_modules/electron/install.js`를 실행하세요.
- `get-windows`: Windows x64용 N-API 프리빌드(`lib/binding/napi-9-win32-unknown-x64`)가 패키지에 포함되어 있어 빌드 스크립트가 필요 없습니다. N-API라서 Electron용으로 다시 빌드하지 않아도 로드됩니다.

## 구조

```
src/
├── main/
│   ├── main.js          진입점: 오버레이 생성, 감지 시작, 종료 단축키
│   ├── activeWindow.js  get-windows 1초 폴링, 변경 시에만 콜백, 자기 프로세스 제외
│   └── overlay.js       투명·항상 위·포커스 없음 창, 클릭 통과 토글 IPC
├── preload/preload.cjs  contextBridge로 window.ghost API만 노출
└── renderer/
    ├── overlay.html     임시 CSS 캐릭터 + 말풍선
    └── overlay.js       캐릭터 위 판정, 드래그·클릭, 활성 창 표시
```

### 클릭 통과 방식

1. 오버레이는 주 모니터 작업 영역 전체를 덮고, 기본값은 `setIgnoreMouseEvents(true, { forward: true })`입니다. 클릭은 뒤쪽 앱으로 가고 mousemove만 렌더러에 전달됩니다.
2. 렌더러는 mousemove의 대상이 `#character` 안인지 검사해서, 바뀌었을 때만 `overlay:set-interactive` IPC를 보냅니다.
3. 커서가 캐릭터 위에 있을 때만 창이 마우스를 받습니다. 드래그 중에는 커서가 캐릭터 밖으로 나가도 계속 받습니다.

## 확인된 사항 / 한계

- ✅ Electron 44 + get-windows 9.3에서 활성 창 감지 동작 (Windows Terminal → 메모장 전환 감지 확인)
- ✅ 렌더러가 캐릭터·말풍선을 정상적으로 그림 (`capturePage`로 확인)
- ⚠️ 창 제목이 계속 바뀌는 앱이 있습니다 (예: 터미널 제목의 스피너 `◐/◑`). 변경 이벤트가 1초마다 발생할 수 있으므로, 판단 단계에서는 앱/제목이 N초 이상 유지될 때만 평가해야 합니다 (AGENTS.md의 15~30초 원칙).
- ⚠️ **브라우저 URL은 Windows에서 제공되지 않습니다** (get-windows의 `url`은 macOS 전용). PC는 앱 이름과 창 제목만으로 판단해야 합니다.
- ⚠️ GDI 방식 스크린 캡처(`CopyFromScreen`/`BitBlt`)에는 투명 오버레이가 찍히지 않습니다. 데모를 녹화할 때는 Windows 캡처 도구나 OBS(Windows Graphics Capture)를 쓰세요.
- 알아둘 점: get-windows는 네이티브 바인딩 로드에 실패해도 에러 없이 `undefined`를 반환합니다. `activeWindow.js`에서 경고를 한 번 출력합니다.

## 수동 검증 체크리스트

- [ ] 오른쪽 아래에 보라색 유령과 말풍선이 보인다
- [ ] 메모장 / Chrome / VS Code / 카카오톡으로 바꿀 때마다 앱 이름·제목이 출력된다
- [ ] 캐릭터를 클릭해도 `[active]`에 GHOST(electron)가 찍히지 않는다
- [ ] 캐릭터 옆 빈 영역과 말풍선 위를 클릭하면 뒤쪽 앱이 반응한다
- [ ] 캐릭터를 클릭하면 찌그러지는 애니메이션이 나오고, 드래그하면 이동한다
- [ ] 다른 창을 최대화해도 캐릭터가 위에 있다
- [ ] 캐릭터가 떠 있는 상태에서 메모장에 타이핑하면 포커스가 유지된다
- [ ] (관찰만) 전체화면 영상/게임, 배율 125%/150%, 모니터 2대, CPU 사용량

## 다음 단계

- 트레이 아이콘 (종료·일시정지)
- 감지 이벤트 정규화 → `shared/api-schema`와 맞추기
- `shared/rules-dictionary.json` 기반 로컬 규칙 판단
- Rive 캐릭터 연동 (캔버스 하나로 그려지므로 클릭 판정은 경계 박스 또는 알파 샘플링)
