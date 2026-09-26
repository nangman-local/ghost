# android/ — Android 클라이언트

Kotlin + Jetpack Compose. 팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.

담당: Android 코어 오너(크리티컬 패스) · Android 서브

**현재 상태:** Spike 1~3 통합 구현. 삼성 실기기에서 **기본 동작만** 확인했다. 서버 연동은 미구현.

전체 인수 검증은 남아 있다 — 미완료 항목은 #2(Chrome 페이지 제목 직접 감지). #1(인수 체크리스트 8개) · #3(탭 인사 말풍선)은 완료됐다.
특히 **Chrome 페이지 제목 직접 감지는 어떤 조건에서도 성공하지 못한다(#2).** 일반 페이지에서도 웹 문서 제목을 읽지 못해
창 제목(`Chrome: Example Domain`)으로 대체한다. 이 값은 창 제목으로 표시하며 페이지 제목으로 가장하지 않는다.
도메인은 일반 탭 · 커스텀 탭 · 시크릿 모드에서 감지되고, 새 탭 · 주소창 편집 중 · 전체 화면에서는 감지되지 않는다.
**시크릿 모드가 일반 탭과 똑같이 감지되는 것은 정책상 재검토 대상이다.**

> 이 폴더는 삭제된 `nangman-local/mobile` 저장소에서 subtree로 편입됐다(`9c81d8e`).
> 편입 이전 커밋의 `(#1)`·`(#2)`·`(#3)`은 **mobile 저장소의 Spike 이슈**이며, 번호가 같은 이 저장소 이슈와 무관하다.

## 빌드

- Android SDK Platform 36 / Build Tools 35.0.0, **JDK 17 또는 21** (JDK 25는 AGP 8.11이 모른다 — `error 25`로 실패한다. 설치·오류는 `README.md`)
- Gradle Wrapper 8.14.3 / AGP 8.11.0 / Kotlin 2.1.20
- `minSdk 26`, `targetSdk 36`

```sh
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

## 구조 규칙

```
MainActivity                     setContent { GhostApp() } 만 둔다
MainViewModel / FloatingLauncher 사용자 시작·종료 명령 (개발자 도구가 사용)
GhostApplication                 프로세스 내부 상태 저장소 + contract 연결 지점(수동 DI)
contract/                        UI ↔ 코어 ↔ 서버 경계 인터페이스 (FocusController · TaskRepository · PermissionStatus)
fake/                            contract의 임시 구현 (서버 연동 전 · UI 단위 테스트용)
data/LocalTaskRepository         기기에 저장하는 TaskRepository (Android 독립, 단위 테스트 대상)
data/TaskStorage                 저장 경계 + 날짜 정리 · 손상값 정리 (순수 Kotlin)
data/PreferencesTaskStorage      SharedPreferences JSON 저장 (Android 구현)
permission/                      PermissionStatus 구현 (시스템 설정 직접 조회)
ui/                              Compose 화면 (navigation · onboarding · home · more · component · theme)
floating/FloatingService         서비스·알림·권한 감시 수명
floating/OverlayController       WindowManager 창 생성·드래그·제거
floating/OverlayBounds           Android 독립 좌표 로직 (단위 테스트 대상)
floating/CharacterAppearance     개입 레벨 → 캐릭터 상태·크기 (Android 독립, 단위 테스트 대상)
floating/NegotiationState        2분 협상 상태 전이 (Android 독립, 단위 테스트 대상, #63)
floating/NegotiationController   NegotiationState 배선 — 개입 레벨·할 일 제목 구독, 카운트다운 틱
detection/UsageAppMonitor        최근 외부 앱 관측
detection/ChromeAccessibilityService  Chrome 이벤트 어댑터
detection/ChromeMetadata         도메인 정제 + 문서 루트 제목
focus/FocusStateMachine          딴짓 누적 → 개입 레벨 승급 (Android 독립, 단위 테스트 대상)
focus/InterventionPolicy         개입 임계값 (기본·데모 모드)
focus/RuleJudge                  로컬 규칙 판단 (FOCUS/DISTRACT/AMBIGUOUS)
focus/GhostFocusController       FocusController 실제 구현 — 감지·상태머신·플로팅 배선
```

- **서비스가 오버레이 창을 소유한다.** ViewModel/Activity에 창이나 Service 인스턴스를 저장하지 않는다.
  개입 레벨 구독도 서비스가 한다(`FloatingService.observeInterventionLevel`) — 창을 가진 쪽이 그린다.
- Android 독립 로직(좌표 계산, 도메인 정제, 상태 전이)은 순수 Kotlin으로 분리해 단위 테스트를 붙인다.

## UI 레이어 규칙

UI(화면)·코어(감지·오버레이·상태머신)·서버 연동을 서로 독립적으로 개발하기 위한 규칙이다.

- **화면 Composable은 `UiState`와 콜백만 받는다.** 서비스·Repository·StateStore를 직접 참조하지 않는다.
- **`ui/`와 그 ViewModel은 `contract/`만 본다.** `floating/`·`detection/`·`data/`를 import하지 않는다.
  - 예외: `ui/more/`의 개발자 도구 화면. 실기기 검증용으로 `FloatingState`·`DetectionState`를 직접 본다.
    단 **시작·종료는 예외가 아니다** — 아래 "세션은 하나다" 참고.
- `FocusSnapshot.distractSeconds`는 **이번 세션의 합계**다(#71). 연속이 아니라 누적이고, 세션이 끝나면 0이다.
  `shared/api-schema.md`의 `devices[].distractSeconds`와 같은 값이라 서버 연동(#14) 때 그대로 올린다.
- **contract 구현 담당:** `FocusController` → Android 코어 오너, `TaskRepository` → 서버·동기화 담당, `PermissionStatus` → UI 담당.
  - Fake를 실제 구현으로 바꿀 때는 `GhostApplication`의 연결부만 고친다.
  - contract를 바꾸면 `shared/`처럼 팀 공유 채널에 공지한다. 세 담당이 같이 쓰는 경계다.
- **세션은 기기 안에 하나다(#52).** 홈이든 개발자 도구든 **`FocusController`를 거쳐** 시작·종료한다.
  `FloatingLauncher`를 직접 부르지 않는다 — 그러면 플로팅 창만 뜨고 세션이 시작되지 않아,
  딴짓을 아무리 해도 누적되지 않고 개입도 일어나지 않는다. 검증할 때 버그와 구분이 안 된다.
  `FloatingLauncher`는 `GhostApplication`이 `GhostFocusController`에 넘기는 용도로만 쓴다.
  `shared/api-schema.md`의 `devices[].state`가 기기당 하나이므로 기기 안에서 먼저 하나여야 한다(서버 연동 #14의 선행 조건).
  규격은 `FocusControllerContract.sessionIsSingleAcrossCallers`에 있다.
  - 개발자 도구에는 선택된 할 일이 없어 진단용 id(`MainViewModel.DIAGNOSTIC_TASK_ID`)로 세션을 연다.
    **서버 연동 때 이 세션을 그대로 올리지 않는다.** 진단용임을 표시하거나 제외한다.
- **`FocusController`의 실제 구현은 `focus/GhostFocusController`다(#61).** `FakeFocusController`는 다른 담당이 UI를 테스트할 때 쓰는 용도로 남겨 둔다.
  서버 연결은 아직 없다(#14). 세션은 프로세스 메모리에만 있고, 프로세스가 죽으면 사라진다.
- **실패는 문구가 아니라 종류(`FocusError`)로 전달한다.** 화면 문구·버튼은 UI가 종류별로 정한다. 코어의 `FloatingState.error`는 아직 문자열이라, 지금은 Fake가 오버레이 권한 여부로 종류를 가른다. 코어가 오류 종류를 직접 내보내면 이 판단은 없앤다.
- **contract 규격 테스트:** `test/.../contract/FocusControllerContract`·`TaskRepositoryContract`에 모든 구현이 지켜야 할 동작이 있다. 실제 구현을 만들면 이 클래스를 상속한 테스트로 자기 구현을 돌린다(예: `FakeFocusControllerTest`). 테스트는 각자 맡은 코드의 담당이 쓴다.
- 접근성 허용 여부는 `AccessibilityManager`의 활성 서비스 목록으로 본다. 서비스 연결 여부(`chromeConnected`)는 프로세스 메모리 값이라 권한 판정에 쓰지 않는다.
- 라우트는 문자열로 둔다(타입 세이프 라우트는 serialization 플러그인이 필요하다).
- 테마 토큰(`ui/theme/`)은 Figma "UI" 페이지(node `1:125`)의 raw 값에서 추출했다. Figma 변수는 heading 크기·행간뿐이다.
- **다크 전용**이다. 창 배경(`@color/ghost_background`)을 Compose 배경과 맞춰 시작 시 흰 화면이 번쩍이지 않게 한다.
- 폰트는 Pretendard 1.3.9 OTF 4종(Light·Regular·SemiBold·Bold, SIL OFL — `third_party/pretendard/LICENSE.txt`). Android용 서브셋 OTF는 배포되지 않아 전체본이다(약 6.3MB).

### Figma → Compose 레이아웃 규칙

Figma 시안은 360×760 한 화면 기준 절대 좌표다. 그대로 옮기면 실기기에서 깨지므로 **좌표는 버리고 구조만 따른다.**

- 화면은 `[상단] → [가운데: 그림 영역 weight] → [하단 고정: 인디케이터·버튼]`. 공간이 모자라면 그림이 먼저 줄고(최소 120dp), 그래도 모자라면 가운데가 스크롤된다. 하단 CTA는 항상 보인다.
- 카드·버튼은 `fillMaxWidth` + 좌우 20dp. 콘텐츠 폭은 최대 480dp로 가운데 정렬한다(폴드·태블릿).
- 버튼에 Figma의 고정 좌우 padding(132)을 쓰지 않는다. 글자는 sp, 14.25는 14sp로 반올림, 줄바꿈 허용.
- 상태바·내비게이션 바 목업은 그리지 않고 `safeDrawingPadding`으로 처리한다.
- 가로 모드는 지원하지 않는다(`screenOrientation="portrait"`). Android 16+ 큰 화면에서는 시스템이 이를 무시하므로 레이아웃은 가로에서도 동작해야 한다.
- Preview로 360×640(글자 1.3·1.5배), 360×780, 411×891, 673×841을 확인한다.

### 홈 (`ui/home/`)

- 3상태: 할 일 없음(입력) → 오늘의 할 일(시작하기) → 진행 중(진행바·나의 기록·그만하기). 상태 계산은 순수 함수 `homeContent`(단위 테스트).
- **"시작하기"는 `FocusController.startTask`, "그만하기"는 `stop()`만 부른다.** 세션만 끝나고 할 일은 남는다. 할 일 완료 처리는 서버 연동 때 정한다.
- **할 일·기록은 기기에 저장된다(#53).** 앱을 껐다 켜도 남는다. `LocalTaskRepository` + `PreferencesTaskStorage`(JSON).
  - **날짜가 바뀌면 '오늘 누적'만 0으로 되돌리고 할 일은 남긴다**(`StoredTasks.forDate`). 기기 시간대를 따른다.
  - 저장값이 깨져 있어도 앱은 열린다 — `sanitize`가 빈 id·없는 현재 할 일을 버리고, `nextId`가 작으면 id 충돌을 막는다.
  - **개인 식별 정보를 저장하지 않는다.** 할 일 제목은 사용자가 쓴 것이고 기기 밖으로 내보내지 않는다.
  - 서버가 진실의 원천이라는 원칙은 그대로다. 이 저장은 서버 연동(#14) 전까지의 임시 보관이고, 연동 후에는 오프라인 캐시가 된다.
  - `FakeTaskRepository`는 UI 단위 테스트용으로 남겨 둔다.
- 할 일을 추가하면 바로 현재 할 일이 된다. 현재 할 일의 진실의 원천은 서버 세션이므로 `TaskRepository.selectTask`는 서버 담당이 구현한다.
- '나의 기록'은 **오늘 누적**을 `H:MM`으로 보여준다(`formatHoursMinutes`).
  딴짓 시간은 오늘 누적(`FocusStats`)과 **이번 세션 누적**(`FocusSnapshot.distractSeconds`) 중 **큰 값**을 쓴다(#71).
  둘은 의미가 다르지만, 서버가 오늘 누적을 내려주기 전까지(#14) 화면에 0이 박혀 있는 것을 막는 임시 보정이다.
  **서버 연동이 되면 이 보정을 없앤다.**
- **진행률·체크포인트(10/30/50%)는 자리표시 값이다.** 의미(시간 기준인지 작업 단계인지)가 정해지지 않았다. 정해지면 contract로 받는다.
- 할 일 행 ›는 할 일 목록·추가 팝업을 연다. 시안이 없어 임시 구성이며, 시안이 나오면 `TaskPickerContent`만 바꾼다(띄우는 방식은 `TaskPickerSheet`).
- '그만하기' 버튼은 Figma '나의 기록' 카드의 빈 박스(56:571) 자리에 임시로 둔 것이다.
- 오류는 종류(`HomeError`: contract의 `FocusError` 또는 설정 화면 열기 실패)로 받고, 문구와 버튼은 화면이 종류별로 정한다. 권한 문제(`OVERLAY_PERMISSION_MISSING`)일 때만 '권한 설정'을 보여준다. 재시도는 사용자가 한다.
- '시작하기'는 권한이 없어도 눌린다(의도). 이슈 #1의 "권한 없음: 시작 비활성"은 개발자 도구 기준이다.
- `HomeViewModel`은 `TaskRepository`·`FocusController`를 생성자로 받는다(앱에서는 `HomeViewModel.Factory`가 `GhostApplication`의 연결을 넣는다). 그래서 Fake로 단위 테스트한다(`HomeViewModelTest`).
- 하단 탭: 캘린더(자리표시) · 홈 · 더보기(개발자 도구). 탭 전환은 중첩 NavHost + `saveState/restoreState`.
- 개발자 도구의 시작·종료 버튼은 **세션 상태**(`FocusSnapshot.state`)로 켜고 끈다. 플로팅 창 가시성이 아니다.
  데모 모드 토글도 여기 있다(#65). `MainViewModel`은 이 기능 때문에 `contract/`가 아닌 구현체를 본다(개발자 도구 예외).
  `SessionPanel`이 세션 상태·개입 레벨을 그대로 보여준다 — 개입이 아직 화면에 아무것도 그리지 않으므로(#16·#63) 실기기 검증(#69)은 이 값을 읽는다.

### 캐릭터 이미지 (Rive 전 임시)

- `ui/component/GhostIllustration`이 포즈(`GhostPose`)별 PNG를 그린다. **Rive(`.riv`)가 나오면 이 Composable 안만 교체한다.**
- PNG는 Figma에서 글로우 포함 3x로 내보냈고 배경색 `#040309`가 들어 있다. 다른 배경 위에 올리지 않는다.
- 온보딩 2번 그림(`onboarding_overlay.png`)에는 제조사 배경화면이 들어 있다. 공개 배포 전 디자인팀이 교체해야 한다.

## 권한

사용 정보 접근(`UsageStatsManager`)과 접근성(`AccessibilityService`)은 **사용자가 시스템 설정에서 직접 허용한다.** 권한 없이도 플로팅은 동작해야 한다.

- 권한 철회/창 연결 실패 시 서비스를 정리하고 앱에 오류를 표시한다. **자동 재시도하지 않는다.**
- **온보딩(`ui/onboarding/`)**: 시작 → 다른 앱 위에 표시 → Chrome 접근성 → 사용 정보 접근 → 알림(API 33+).
  - 모든 권한 단계에 "나중에"가 있다. 이미 허용된 단계는 건너뛰고, 설정에서 돌아왔을 때(onResume) 허용됐으면 다음 단계로 간다.
  - 첫 실행 때 한 번만 보여준다(SharedPreferences `ghost_ui/onboarding_done`). 서버 세션과 무관한 UI 플래그다.
  - 오버레이를 건너뛰면 플로팅은 뜨지 않는다. "권한 없이도 동작"이 보장되는 건 사용 정보·접근성뿐이다.
  - **단계는 표 하나(`OnboardingSteps.all`)로 정의한다.** 단계마다 문구·그림·권한·받는 방법(`GrantMethod`: 설정 화면 열기 / 런타임 팝업)을 한곳에 둔다. 새 권한 단계(예: 배터리 최적화 예외)는 `contract/Permission` 값 + `AndroidPermissionStatus` 확인 + 표 한 줄 + 문자열만 추가한다. 인디케이터·건너뛰기·자동 진행·뒤로가기는 `OnboardingFlow`가 맞춘다.
  - 런타임 권한 단계는 `minSdk` 미만 기기에서 없어진다(알림은 API 33). 인디케이터 개수도 따라 줄어든다.
- **알림 권한(`POST_NOTIFICATIONS`)을 매니페스트에 선언하고 온보딩에서 런타임 요청한다.** 이 권한이 허용되면 `FloatingService`의 FGS 알림이 실제로 표시된다(선언 전에는 Android 13+에서 숨겨졌다).
  - 두 번 거부해 팝업이 더 뜨지 않으면(`shouldShowRequestPermissionRationale == false`) 앱 알림 설정을 연다.
- **Play 정책:** 접근성·사용 정보 접근은 요청 **전에** 무엇을 읽고 무엇을 읽지 않는지 눈에 띄게 고지해야 한다. 지금 온보딩 문구는 Figma를 따른 것이라 이 요구를 충족하지 않는다. 공개 배포 전 #23에서 문구를 확정한다. 상세 고지는 더보기 > 개발자 도구에 남아 있다.
- `START_NOT_STICKY`. 부팅 수신자·자동 복구 없음.

## 개입 상태머신 (`focus/`)

**이 서비스의 핵심 가설을 구현한 곳이다.** 딴짓을 누적해 개입 레벨을 올린다.

- **판단 로직은 전부 `FocusStateMachine`에 있다.** Android 타입이 들어오지 않아 단위 테스트로 전부 검증한다(`FocusStateMachineTest`).
  `GhostFocusController`는 배선만 한다 — 감지 관측을 `RuleJudge`로 판정해 상태머신에 넣고, 결과를 `FocusSnapshot`으로 내보낸다.
- **임계값은 `shared/states.md`의 "개입 전이 규칙" 표가 진실의 원천이다.** `InterventionPolicy`가 같은 값을 들고 있다.
  한쪽만 고치지 않는다. **PC와도 같은 값을 써야 한다.**
- **지금 값은 임시값이다.** 예소팀 7명이 2026-09-24부터 도그푸딩하며 조정한다(#10). 조정되면 `InterventionPolicy` 기본값과 `shared/states.md`를 같이 고친다.
- **임계값을 하드코딩하지 않는다.** `InterventionPolicy`를 주입받는다 — 데모 모드(#65)가 같은 로직을 초 단위로 돌린다.
- **데모 모드는 더보기 > 개발자 도구에서 켠다(#65).** `FocusStateMachine.changePolicy`가 임계값을 바꾸고 **진행 중인 세션을 끝낸다** —
  10분 기준으로 쌓인 누적을 30초 기준에 그대로 넘기면 켜자마자 최고 단계로 뛴다.
  **저장하지 않는다.** 앱을 다시 켜면 기본 모드다 — 실수로 켜둔 채 배포되는 것을 막는다.
- **MVP는 2단계까지다**(`maxLevel = 2`). 3단계 화면 가리기는 #22에서 터치 차단 범위를 설계한 뒤에 올린다.
- **누적은 연속이 아니라 세션 합계다.** 잠깐 복귀했다 돌아와도 누적이 이어진다. 유예 시간(30초) 이상 집중해야 리셋된다.
- **`minHoldSeconds`만큼을 누적에서 빼지 않는다.** 판정이 확정될 때까지 시간을 흘려보내지 않고 기다렸다가 한꺼번에 누적한다.
  빼면 10분을 봤는데 누적이 9분 40초가 되어 "유튜브 본 지 10분 됐어"가 거짓말이 된다(`누적은 유지 시간만큼 깎이지 않는다` 테스트).
- **감지 실패는 딴짓이 아니다.** 관측이 없거나 `AMBIGUOUS`면 누적하지 않고 개입하지도 않는다. 오탐이 미탐보다 훨씬 치명적이다.
- **틱 루프는 세션이 끝나면 같이 끝난다.** 대기 중에 타이머를 돌리지 않는다. 끝나지 않는 루프는 테스트에서 `advanceUntilIdle`을 멈추지 못하게 만든다.
- `RuleJudge`는 지금 `shared/distract-rules.json` 값을 상수로 들고 있다. 자산 파일을 읽는 방식은 서버가 규칙을 내려주는 #12 때 정한다.
  **값을 고칠 때는 `shared/distract-rules.json`을 먼저 고치고 반영한다.**

## 2분 협상 (`floating/NegotiationState`·`NegotiationController`, #63)

`shared/states.md`의 `interventionLevel = 2`("복귀 제안")를 실제로 채우는 곳이다.

- **딴짓 판단과 완전히 분리한다.** `NegotiationMachine`은 `FocusStateMachine`을 모른다.
  개입 레벨·할 일 제목만 구독해 무엇을 보여줄지만 정한다. **거절해도 레벨·누적은 그대로다** —
  협상은 화면 위에 얹힌 표시일 뿐, 딴짓 판단 자체를 바꾸지 않는다.
- **레벨 2가 된다고 자동으로 뜨지 않는다. 유령을 탭해야 연다(`NegotiationController.reveal`).**
  처음엔 레벨 2가 되면 바로 말풍선을 띄웠는데, 유령이 드래그돼 있던 화면 아무 자리에서
  예고 없이 튀어나와 하필 중요한 내용을 가릴 수 있다는 문제가 있었다. 캐릭터 표현(눈 커짐 등,
  `CharacterAppearance`)으로 레벨이 올랐다는 신호는 주되, 내용을 볼지는 사용자가 정한다.
  탭 인터랙션(#64: "1회 터치 → 현재 할 일·진행 상태")과도 이 지점에서 자연스럽게 이어진다 —
  세션 중 탭하면 레벨 2일 때만 이 제안이 뜨고, 아니면 기존 "안녕?"으로 넘어간다.
- **상태:** `Hidden → Offer → Counting → Celebrate`. 레벨이 2 밑으로 내려가면(복귀) 어느 단계에
  있든 `Hidden`으로 정리한다. 거절하면 `Hidden`으로 돌아가지만 **레벨이 2인 동안은 "열어볼 수
  있다"는 사실 자체는 남아 있다** — 다시 탭하면 또 열린다. 사용자가 스스로 다시 확인하는 것까지
  막을 이유는 없다.
- **`Offer`는 처음엔 "해볼래"만 보여준다.** 거절 선택지(`rejectVisible`)는 **유령을 한 번 더
  탭해야**(`NegotiationMachine.revealReject`) 나온다. 수락 쪽으로 살짝 기울이는 장치이지,
  거부 자체를 막는 게 아니다 — `reject()`는 `rejectVisible`이 아니면 무시한다(화면에 없던
  버튼이 눌릴 수 없다는 걸 로직에도 반영). `Celebrate`의 "쉴래"는 이 2단계 노출을 쓰지 않고
  처음부터 보인다 — 이번엔 "아니, 됐어" 범위만 다뤘다.
- **거절 시 스누즈 연계는 아직 없다(#64 선행).** 지금은 거절 = 그냥 닫기다. #64가 구현되면
  거절을 스누즈로 이어붙일지는 그때 정한다.
- **주 동작(수락/더 할래)만 버튼으로 강조하고, 보조 동작(거절/쉴래)은 작은 텍스트로 낮춘다.**
  거부할 수단 자체를 없애지 않는다 — #64: "개입을 거부할 출구가 없으면 사용자는 앱을 지운다."
  텍스트가 작아도 히트 영역은 최소 32dp를 보장한다(`CharacterView.drawNegotiationBubble`).
- **카운트다운은 순수 Kotlin이다(`NegotiationMachine.tick`).** `GhostFocusController`의 틱과
  같은 자리에 별도로 돈다 — 딴짓 누적 틱과 합치지 않는다(관심사가 다르다).
  **데모 모드(#65)는 아직 이 카운트다운에 적용되지 않는다** — 항상 120초다. 개입 레벨은
  데모에서 초 단위로 빨라지는데 협상만 2분 그대로라 시연 흐름이 어긋날 수 있다(후속 과제).
- **할 일 제목이 없으면 대체 문구를 쓴다.** 개발자 도구의 진단 세션(`DIAGNOSTIC_TASK_ID`)은
  실제 할 일 목록에 없어 제목을 못 구한다.
- **창을 서비스가 그린다.** `applyAppearance`(레벨→표현)와 같은 원칙 — `FloatingService`가
  `NegotiationController.state`를 구독해 `OverlayController.applyNegotiation`을 부른다.
- **첫 행동 문구는 고정 템플릿이다.** AI Task Breakdown(#12) 없이 "[할 일] 딱 2분만 해볼까?"
  하나로 시작한다.

## 오버레이 규칙

- **Android 12 이상은 불투명 오버레이 아래로 터치가 전달되지 않는다.** 개입 3단계의 "콘텐츠 일부 가리기"는 **가린 영역만** 터치를 막고 나머지 화면은 정상 동작해야 한다. 클릭을 완전히 막지 않는다.
- 전체 화면 투명 창을 만들지 않는다. 캐릭터 크기의 작은 창만 만든다.
- 투명한 여백도 드래그 영역에 포함된다.
- **협상 중에는 창이 `NEGOTIATION_WIDTH_DP`(240dp)까지 넓어진다(#63).** 말풍선·버튼을 담기에
  캐릭터(80dp)만으로는 부족해서다.
- **협상 중인 창은 유령이 있던 자리가 아니라 화면 정중앙의 고정된 자리에 뜬다**
  (`OverlayBounds.centered`). 유령 위치 그대로 키우면 화면 어디서나(하필 중요한 내용
  위일 수도 있게) 예측 불가능하게 떴다 — 대신 항상 같은 안전한 자리로 옮기고, 협상이
  끝나면(수락 완료·거절·복귀) `OverlayController.positionBeforeNegotiation`에 저장해 둔
  원래 자리로 되돌린다. 레벨에 따른 일반적인 크기 변화(`applyAppearance`)는 여전히 캐릭터를
  창의 오른쪽 아래에 고정한 채 왼쪽·위로 자라는 기존 방식을 쓴다 — 고정 위치는 **협상 중일
  때만** 적용된다.
- **협상 중 캐릭터 몸을 한 번 더 탭하면(`CharacterView.negotiationCharacterAreaAt`) 거절
  선택지를 연다.** 버튼이 아닌 말풍선 여백을 눌렀을 때와는 구분한다 — 여백은 무시하고,
  캐릭터 몸(오른쪽 아래 80×88dp)만 이 반응을 한다.
- **캐릭터 그림은 협상 중에도 항상 80×88dp로, 창의 오른쪽 아래에 그린다.** 가로(`CHARACTER_SIZE_DP`,
  80)와 세로(`CHARACTER_HEIGHT_DP`, 88)가 다르다 — 발 부분이 아래로 더 나온다. 세로에도
  `CHARACTER_SIZE_DP`를 쓰면 발이 창 밖으로 잘린다(실기기에서 실제로 겪음).
- **"안녕?" 말풍선과 협상 UI는 배타적이다.** 창 크기 계산(`OverlayController.applyLayout`)도
  이 배타성을 지켜야 한다 — 안 지키면 협상 중에도 "안녕?"의 38dp 여백이 남아 말풍선과
  캐릭터 사이가 벌어진다(실기기에서 실제로 겪음: 유령을 톡 건드려 인사 여백이 켜진 채로
  레벨 2에 도달하면 재현된다).
- **버튼 히트 테스트는 `CharacterView`가 직접 한다.** Compose를 오버레이 창에 넣지 않고,
  기존 Canvas 그리기 방식(`android/AGENTS.md`가 이미 그렇게 해 온 방식)을 그대로 따른다 —
  그릴 때 버튼 사각형을 기록해 뒀다가 `ACTION_UP`에서 그 사각형 안인지 본다.
  **협상 중에는 버튼 밖을 눌러도 인사(안녕?)로 새지 않는다** — `showGreeting()`의 창 크기
  계산은 협상용 크기와 다른 가정을 쓰기 때문에 섞이면 창 크기가 꼬인다.

## 감지 규칙

- `shared/distract-rules.json`의 `android` 섹션과 `minHoldSeconds`를 따른다.
- **도메인과 제목만** 서버로 보낸다. URL 쿼리스트링과 화면 텍스트는 보내지 않는다.
- 접근성 서비스는 문서 루트 제목만 읽는다. **본문 탐색을 하지 않는다.**
- **커스텀 탭은 트리 탐색으로 주소창에 닿지 못한다.** 활성 창 루트 아래에서 `url_bar`를 만나지 못하므로,
  트리 탐색으로 도메인을 못 찾았을 때만 `url_bar` 뷰 ID로 직접 조회한다. 본문 노드는 지나가지 않는다.
- **커스텀 탭 주소창은 화면에 보여도 `isVisibleToUser=false`로 보고된다**(SM-G998N / Android 15 / Chrome 153 확인).
  그래서 뷰 ID 직접 조회 경로에서는 가시성을 조건에 넣지 않는다. 편집 중(`focused`)·비밀번호·다른 앱 노드는 그대로 제외한다.
- **검증에 `uiautomator dump`를 쓰지 않는다.** 덤프를 뜨는 순간 이 접근성 서비스의 연결이 끊겨(`연결 안 됨`) 관측이 멈춘다.
  화면 캡처로 확인한다.

## 완료 기준

**실기기에서 확인한 것만 완료다.** 에뮬레이터는 완료가 아니며 삼성 기기 최소 1대 확인이 필요하다.
크리티컬 패스(감지·오버레이·상태머신) 변경은 Android 코어 오너 확인 후 머지한다.
