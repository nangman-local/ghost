# android/ — Android 클라이언트

Kotlin + Jetpack Compose. 팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.

담당: Android 코어 오너(크리티컬 패스) · Android 서브

**현재 상태:** Spike 1~3 통합 구현. 삼성 실기기에서 **기본 동작만** 확인했다. 서버 연동은 미구현.

전체 인수 검증은 남아 있다 — 미완료 항목은 #1(인수 체크리스트 8개) · #2(Chrome 페이지 제목 직접 감지) · #3(탭 인사 말풍선).
특히 **Chrome 페이지 제목 직접 감지는 미검증이다.** 실기기에서 관측된 `Chrome: Google` 같은 값은 창 제목 대체값이며,
페이지 제목으로 가장하지 않는다.

> 이 폴더는 삭제된 `nangman-local/mobile` 저장소에서 subtree로 편입됐다(`9c81d8e`).
> 편입 이전 커밋의 `(#1)`·`(#2)`·`(#3)`은 **mobile 저장소의 Spike 이슈**이며, 번호가 같은 이 저장소 이슈와 무관하다.

## 빌드

- Android SDK Platform 36 / Build Tools 35.0.0, JDK 17 이상
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
fake/                            contract의 임시 구현 (서버·상태머신 연동 전)
permission/                      PermissionStatus 구현 (시스템 설정 직접 조회)
ui/                              Compose 화면 (navigation · onboarding · home · more · component · theme)
floating/FloatingService         서비스·알림·권한 감시 수명
floating/OverlayController       WindowManager 창 생성·드래그·제거
floating/OverlayBounds           Android 독립 좌표 로직 (단위 테스트 대상)
detection/UsageAppMonitor        최근 외부 앱 관측
detection/ChromeAccessibilityService  Chrome 이벤트 어댑터
detection/ChromeMetadata         도메인 정제 + 문서 루트 제목
```

- **서비스가 오버레이 창을 소유한다.** ViewModel/Activity에 창이나 Service 인스턴스를 저장하지 않는다.
- Android 독립 로직(좌표 계산, 도메인 정제, 상태 전이)은 순수 Kotlin으로 분리해 단위 테스트를 붙인다.

## UI 레이어 규칙

UI(화면)·코어(감지·오버레이·상태머신)·서버 연동을 서로 독립적으로 개발하기 위한 규칙이다.

- **화면 Composable은 `UiState`와 콜백만 받는다.** 서비스·Repository·StateStore를 직접 참조하지 않는다.
- **`ui/`와 그 ViewModel은 `contract/`만 본다.** `floating/`·`detection/`·`data/`를 import하지 않는다.
  - 예외: `ui/more/`의 개발자 도구 화면. 실기기 검증용 기존 화면(`DetectionPanel`, 시작·종료)을 그대로 쓴다.
- **contract 구현 담당:** `FocusController` → Android 코어 오너, `TaskRepository` → 서버·동기화 담당, `PermissionStatus` → UI 담당.
  - Fake를 실제 구현으로 바꿀 때는 `GhostApplication`의 연결부만 고친다.
  - contract를 바꾸면 `shared/`처럼 팀 공유 채널에 공지한다. 세 담당이 같이 쓰는 경계다.
- `FakeFocusController`는 세션 상태를 메모리에만 두고 기존 플로팅 시작·종료만 호출한다. **세션·상태머신·서버 연결은 코어 오너 TODO다.**
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
- **알림 권한(`POST_NOTIFICATIONS`)을 매니페스트에 선언하고 온보딩에서 런타임 요청한다.** 이 권한이 허용되면 `FloatingService`의 FGS 알림이 실제로 표시된다(선언 전에는 Android 13+에서 숨겨졌다).
  - 두 번 거부해 팝업이 더 뜨지 않으면(`shouldShowRequestPermissionRationale == false`) 앱 알림 설정을 연다.
- **Play 정책:** 접근성·사용 정보 접근은 요청 **전에** 무엇을 읽고 무엇을 읽지 않는지 눈에 띄게 고지해야 한다. 지금 온보딩 문구는 Figma를 따른 것이라 이 요구를 충족하지 않는다. 공개 배포 전 #23에서 문구를 확정한다. 상세 고지는 더보기 > 개발자 도구에 남아 있다.
- `START_NOT_STICKY`. 부팅 수신자·자동 복구 없음.
- Android 13+ 알림 권한은 시스템 설정에 따른다.

## 오버레이 규칙

- **Android 12 이상은 불투명 오버레이 아래로 터치가 전달되지 않는다.** 개입 3단계의 "콘텐츠 일부 가리기"는 **가린 영역만** 터치를 막고 나머지 화면은 정상 동작해야 한다. 클릭을 완전히 막지 않는다.
- 전체 화면 투명 창을 만들지 않는다. 캐릭터 크기의 작은 창만 만든다.
- 투명한 여백도 드래그 영역에 포함된다.

## 감지 규칙

- `shared/distract-rules.json`의 `android` 섹션과 `minHoldSeconds`를 따른다.
- **도메인과 제목만** 서버로 보낸다. URL 쿼리스트링과 화면 텍스트는 보내지 않는다.
- 접근성 서비스는 문서 루트 제목만 읽는다. **본문 탐색을 하지 않는다.**

## 완료 기준

**실기기에서 확인한 것만 완료다.** 에뮬레이터는 완료가 아니며 삼성 기기 최소 1대 확인이 필요하다.
크리티컬 패스(감지·오버레이·상태머신) 변경은 Android 코어 오너 확인 후 머지한다.
