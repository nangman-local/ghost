# GHOST Android

[GHOST](../README.md) 모노레포의 **Android 클라이언트** (Kotlin + Jetpack Compose).

- 담당: Android 코어 오너 / Android 서브
- 역할: 포그라운드 서비스, `UsageStatsManager` 기반 현재 앱 감지, `AccessibilityService` 기반 브라우저 도메인/제목 감지, `WindowManager` 오버레이 플로팅 캐릭터, 개입 상태머신, 캘린더 읽기
- 상태: Spike 1~3 통합 구현(플로팅 + 최근 앱 + Chrome 도메인/제목 관측) 위에 Figma 기준 UI(온보딩·홈·하단 탭)를 얹었다. 삼성 실기기에서는 Spike의 기본 동작만 확인했고, 새 UI는 실기기 검증 전이다. 서버 연동은 미구현이며 홈 데이터는 임시(Fake) 구현으로 채운다.
- 규칙: [`AGENTS.md`](./AGENTS.md) — 작업 전 루트 [`AGENTS.md`](../AGENTS.md)도 읽으세요

이 폴더는 GHOST의 Android 기술 스파이크 단계다. **서버 세션이 진실의 원천이며 이 로컬 진단 상태가 서버 세션을 대체하지 않는다.** 감지·오버레이 변경은 Android 코어 오너 리뷰 후 머지한다.

## 앱 화면 (Figma UI 페이지 기준)

```text
앱 실행 ─ 온보딩을 마쳤나? ─ 아니오 → 온보딩: 시작 → 다른 앱 위에 표시 → Chrome 접근성 → 사용 정보 접근 → 알림(Android 13+)
                         └ 예 ───→ 메인 탭: 캘린더 · 홈 · 더보기
```

- **온보딩**(첫 실행 때 한 번): 권한 화면마다 "나중에"로 건너뛸 수 있다. 설정에서 허용하고 돌아오면 자동으로 다음 단계로 간다. 이미 허용된 단계는 건너뛴다. 완료 여부는 기기에만 저장한다(앱 데이터를 지우면 다시 나온다).
- **홈**: 할 일 없음(입력) → 오늘의 할 일(시작하기) → 진행 중(진행바·나의 기록·그만하기). '시작하기'는 할 일 세션을 시작하면서 플로팅을 띄우고, '그만하기'는 플로팅을 끄고 '오늘의 할 일'로 돌아간다. 할 일 행의 ›를 누르면 할 일 목록·추가 팝업이 뜬다.
- **캘린더**: 자리만 있다.
- **더보기 > 개발자 도구**: 아래 "현재 프로토타입 범위"의 기존 제어 화면(권한 안내·시작/종료·감지 패널)을 그대로 옮겨 둔 곳이다. **Spike 검증은 이 화면에서 한다.**
- 할 일·기록·진행률은 서버 연동 전까지 메모리에만 있는 임시 값이다. 앱을 종료하면 사라진다.
- 가로 모드는 지원하지 않는다(세로 고정). 다크 테마 전용이다.

코드 구조와 담당 경계(`contract/`)는 [`AGENTS.md`](./AGENTS.md)의 "UI 레이어 규칙"을 본다.

## 현재 프로토타입 범위

아래는 Spike 단계의 범위이며, 지금은 **더보기 > 개발자 도구** 화면에 해당한다.

사용자 승인: **권한 안내 + 시작/종료, 다른 앱 위 임시 유령, 드래그 이동, 창 영역 밖 터치 전달**. 후속 승인으로 **유령을 탭하면 “안녕?” 말풍선 표시**를 추가했다.

- Kotlin + Compose 단일 `app` 모듈. 캐릭터는 Android Canvas로 그린 임시 자산이며 Rive는 아직 연결하지 않았다.
- 창 크기는 80 × 88 dp, 초기 위치는 화면 우측 중앙이다. 탭하면 위쪽에 말풍선을 붙여 80 × 126 dp로 확장한다. 화면 경계가 아니라면 유령 자체 위치는 유지된다. 터치 영역은 이 작은 사각형 창이며 투명한 여백도 드래그 영역에 포함된다. 전체 화면 투명 창을 만들지 않는다.
- 탭하면 “안녕?” 말풍선이 나타나 종료까지 유지되고, 유령과 함께 드래그된다. 반복 탭은 말풍선을 중복 생성하지 않는다. 드래그·취소 입력으로는 인사하지 않는다. 종료 후 위치와 말풍선은 초기화된다.
- 후속 승인으로 Spike 2(최근 외부 앱)·Spike 3(Chrome 도메인/제목)를 기존 제어 화면 아래에 추가했다. 시작 중에만 메모리에서 관측하고 종료하면 초기화한다. 사용 정보 접근/접근성은 사용자가 시스템 설정에서 직접 허용한다. 권한 없이도 Spike 1은 동작한다.
- 자동 이동, 위치/관측 결과 저장, 자동 재시작, 딴짓 판단·자동 개입, 서버·AI·PC 연동은 없다. INTERNET 권한도 없다.
- 시작 버튼으로 서비스를 실행하고 종료 버튼으로 창과 서비스를 제거한다. Activity 재생성과 앱 전환은 서비스 수명과 별개다.
- `START_NOT_STICKY`이며 부팅 수신자·자동 복구 작업이 없다. 프로세스 종료 후에는 앱을 열어 다시 시작해야 한다. 최근 앱에서 화면을 제거하는 것과 서비스 종료는 OS에 따라 다를 수 있으므로 명시적 종료 버튼으로 검증한다.
- 표시 권한 철회/창 연결 실패 시 서비스를 정리하고 앱에 오류를 표시한다. 자동 재시도하지 않는다.
- Android 13+ 알림 권한(`POST_NOTIFICATIONS`)은 온보딩 마지막 단계에서 요청한다. 허용하면 포그라운드 서비스 알림이 알림 서랍에 보이고, 거부하면 이전처럼 보이지 않는다. 종료는 홈의 '그만하기' 또는 개발자 도구의 종료 버튼으로 한다.

## 구조

```text
MainActivity                  setContent { GhostApp() } 만 둔다
ui/                           Compose 화면: onboarding · home · calendar · more(개발자 도구) · component · theme · navigation
contract/                     UI ↔ 코어 ↔ 서버 경계 인터페이스 (FocusController · TaskRepository · PermissionStatus)
fake/                         contract 임시 구현 (서버·상태머신 연동 전)
permission/                   PermissionStatus 구현 (시스템 설정 직접 조회)
MainViewModel / FloatingLauncher  개발자 도구의 시작·종료 명령
GhostApplication              프로세스 내부 FloatingStateStore + DetectionStateStore + contract 연결 지점
floating/FloatingService      서비스·알림·권한 감시 수명
floating/OverlayController    WindowManager 창 생성·드래그·제거
floating/FloatingState        표시/위치/오류 상태
floating/OverlayBounds        Android와 독립적인 좌표 제한 로직
floating/CharacterView        임시 유령 렌더링
detection/UsageAppMonitor      서비스 수명 내 최근 외부 앱 관측
detection/ChromeAccessibilityService  사용자 허용 후 Chrome 이벤트 어댑터
detection/ChromeMetadata      도메인 정제 + 문서 루트 제목 읽기, 본문 탐색 차단
detection/DetectionPanel       최근 관측/권한/미감지 상태 표시
```

서비스가 오버레이 창을 소유한다. ViewModel/Activity에는 창이나 Service 인스턴스를 저장하지 않는다. 이 로컬 표시 상태는 향후 서버 세션을 대체하지 않는다.

## 빌드 및 설치

- Android SDK Platform 36 / Build Tools 35.0.0.
- **JDK 17 또는 21.** JDK 25에서는 빌드가 실패한다(아래 표 참고). AGP 8.11이 아직 모르는 버전이다.
- Gradle Wrapper 8.14.3 / AGP 8.11.0 / Kotlin 2.1.20.
- `minSdk 26`, `targetSdk 36`. Android 8~16 전체 실기기 호환성이 검증됐다는 뜻은 아니다.
- Android Studio에서 `android/` 폴더를 열거나 아래 명령을 사용한다.

### Android Studio 없이 SDK만 설치 (macOS)

```sh
brew install --cask android-commandlinetools
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
yes | sdkmanager --licenses
sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;35.0.0"
```

`sdkmanager`가 "SDK XML versions up to 3 / version 4 was encountered" 경고를 내도 빌드는 정상 동작한다.

### 자주 나는 오류

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `* What went wrong:` 뒤에 숫자 `25` 하나만 나옴 | JDK 25로 실행 | `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` |
| `SDK location not found` | `ANDROID_HOME` 미설정 | 위 설치 절차 참고 또는 `local.properties`에 `sdk.dir=` 지정 |

```sh
# android/ 폴더에서 실행
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties`에 `sdk.dir=...`를 지정해도 된다. 로컬 SDK 경로, 빌드 산출물, 서명 키는 커밋하지 않는다. USB 기기를 여러 대 연결했다면 `adb -s <serial>`로 대상을 지정한다.

## 사용 방법

앱 이름은 **Ghost**다.

**처음 실행 (온보딩)**

1. "Ghost 시작하기"를 누른다.
2. "다른 앱 위에 표시 권한 허용" → 설정에서 Ghost를 허용하고 돌아오면 다음 단계로 넘어간다. OS 버전에 따라 앱 목록에서 Ghost를 선택해야 할 수 있다.
3. "Chrome 접근성 권한 허용" → 접근성의 설치된 앱에서 **GHOST Chrome 감지 테스트**를 켠다. "사용 정보 접근 권한 허용" → Ghost를 허용한다.
4. Android 13 이상이면 "알림 허용"에서 시스템 팝업을 허용한다.
5. 어느 단계든 "나중에"로 건너뛸 수 있다. 단, 다른 앱 위 표시를 건너뛰면 플로팅이 뜨지 않는다.

**홈에서 쓰기**

1. 할 일을 입력하고 "할 일 추가하기"를 누른다. 권한 허용만으로 자동 시작하지 않는다.
2. "시작하기"를 누르면 플로팅이 뜬다. 다른 앱으로 가서 유령을 드래그하거나 탭해 "안녕?" 말풍선을 확인한다. 유령/말풍선 창 바깥의 버튼·스크롤이 정상 작동하는지 확인한다.
3. Ghost로 돌아와 "그만하기"를 누르면 플로팅이 사라진다. 다시 시작하면 이전 위치가 아닌 초기 위치에서 표시된다.

**Spike 검증 (더보기 > 개발자 도구)**

기존 방식 그대로다. **표시 권한 설정** → 허용 후 복귀 → **시작** → 플로팅·감지 패널 확인 → **종료**. 아래 체크리스트와 "휴대폰에서 한 번에 확인할 순서"의 **시작·종료 버튼은 이 화면의 버튼**을 말한다.

> 개발자 도구의 시작·종료와 홈의 시작하기·그만하기는 지금 서로의 상태를 공유하지 않는다(홈 세션은 임시 구현). 한 검증 안에서는 한쪽 버튼만 쓴다.

## 검증

단위 테스트는 초기 좌표, 화면 경계/회전 후 보정, 작은 화면, 상태 전이, 종료 후 위치 제거, 오류 보존을 검사한다.

### 2026-09-12 확인 결과

- 단위 테스트 11개, `lintDebug`(오류 없음, 권고 경고 있음), debug APK 및 계측 테스트 APK 빌드 통과.
- 삼성 SM-G998N / Android 15(API 35)에 설치 및 실행.
- 실기기에서 발견된 Service의 비시각 Context 사용으로 인한 시작 크래시 수정: 기본 Display에 연결한 Context에서 WindowContext를 생성한다. 창 초기화도 서비스의 보호된 시작 경로 안으로 옮겼다.
- 같은 실기기의 `OverlayControllerDeviceTest` 1개 통과: Service Context에서 창 생성, 중복 show, 화면 범위 보정, 반복 hide 및 위치 제거.
- 실제 Foreground Service의 `isForeground=true`, 오버레이 `isVisible=true`/`HAS_DRAWN` 확인. ADB 드래그 전후 창 좌표 변경 확인.
- 계측 테스트 재현: 사용자 표시 권한 허용 후 `./gradlew :app:connectedDebugAndroidTest` (대상 기기는 `ANDROID_SERIAL`로 지정).
- 전체 인수 체크리스트와 실제 화면 대조는 아직 미완료. 필수 `visual-verdict` 스킬은 설치되어 있지 않아 실행하지 못했다. 자동 이동은 현재 구현 범위에 없다. 탭 인사 후속 검증 결과는 아래에 별도로 기록한다.

탭 인사 후속 검증:

- 단위 테스트 13개, lint 오류 0개(권고 경고 11개), 앱/계측 APK 빌드 통과.
- 같은 삼성 실기기에 업데이트 설치, 계측 테스트 2개 통과. 실제 WindowManager 창에서 탭 인사, 드래그/취소 시 인사 억제, 반복 탭 시 위치 유지, 종료/재시작 시 말풍선 초기화를 검사했다.
- 앱 제어 화면은 실기기 캡처로 확인했다. 별도 ADB 탭 전후 크기 비교는 사용자 조작과 겹쳐 단정하지 않았으며, 마지막 말풍선 시각 캡처 전 기기 연결이 해제되어 말풍선 육안 검증은 남아 있다.

삼성 실기기 인수 체크리스트:

- [ ] 권한 없음: 시작 비활성, 설정 복귀 후 권한 상태 갱신, 허용만으로 자동 실행되지 않음.
- [ ] 시작/종료 10회: 매번 창 하나만 생성되고 종료 후 창과 서비스가 남지 않음.
- [ ] 다른 앱 3개 이상 전환: 계속 표시되며 키보드 입력·바깥 버튼·스크롤이 정상 동작함.
- [ ] 드래그: 네 모서리 이동, 손가락 떼기/취소, 화면 회전 후 화면 안에 유지됨. 드래그·취소는 인사하지 않으며 탭만 “안녕?”을 표시함. 말풍선도 함께 이동하고 경계를 벗어나지 않음.
- [ ] 종료 후 다시 시작: 위치 저장 없음. 강제 종료 후 재실행: 자동 표시 없음.
- [ ] 표시 중 권한 철회: 창/서비스가 사라지고 재허용만으로 다시 뜨지 않음.
- [ ] 잠금/해제·최근 앱 제거·시스템 서비스 중단 동작을 관측하고 기기/OS와 함께 기록함. 자동 재시작 없음.
- [ ] 실제 화면을 승인한 작은 시안(권한 안내·시작/종료·임시 유령)과 비교함.

위 체크리스트는 **더보기 > 개발자 도구** 화면 기준이다. 새 UI(온보딩·홈)는 아래를 따로 확인한다.

새 UI 체크리스트 (삼성 실기기, 기본 설정과 '글자 크기 최대'·'화면 크게 보기', 3버튼·제스처 내비게이션):

- [ ] 앱 데이터 삭제 후 첫 실행: 온보딩이 나오고, 이미 허용된 권한 단계는 건너뛴다.
- [ ] 각 권한: 설정에서 허용하고 돌아오면 자동으로 다음 단계. "나중에"로 끝까지 가면 홈에 들어간다. 재실행하면 온보딩을 건너뛴다.
- [ ] Android 13+: 알림 팝업이 뜬다. 두 번 거부한 뒤 누르면 앱 알림 설정이 열린다.
- [ ] 뒤로가기(아이콘·시스템)가 이전 단계로 간다. 시작 화면에서는 앱이 닫힌다.
- [ ] 홈: 추가 → 오늘의 할 일 → 시작하기(플로팅 표시) → 그만하기(플로팅 사라짐, 오늘의 할 일로 복귀). 키보드가 입력칸을 가리지 않는다.
- [ ] 오버레이 권한 없이 시작하기: 오류와 '권한 설정'이 보이고 자동 재시도는 없다.
- [ ] 할 일 팝업에서 선택·추가, 하단 탭 전환이 된다.
- [ ] 작은 화면·큰 글꼴에서 글자·버튼이 잘리지 않는다(필요하면 스크롤된다). 상태바·내비게이션 바와 겹치지 않는다.

기기 모델, OS, APK 버전, 각 항목의 결과와 영상/스크린샷을 함께 남긴다. 에뮬레이터 성공은 삼성 실기기 완료를 대체하지 않는다. 감지·오버레이·상태머신 변경은 Android 코어 오너 검토 후 머지한다.

## 플랫폼 제약

`TYPE_APPLICATION_OVERLAY`는 다른 앱 위 표시용이며 상태 표시줄·민감한 시스템 화면 등 모든 화면 위 표시를 보장하지 않는다. 작은 터치 가능한 창 + `FLAG_NOT_FOCUSABLE`/`FLAG_NOT_TOUCH_MODAL`을 사용하며, 불투명 전체 화면 창으로 터치를 우회하지 않는다.

포그라운드 서비스의 `specialUse` 유형에는 실제 사용 목적을 Manifest에 기재했다. 내부 프로토타입 빌드이며 Google Play의 유형/권한 심사 통과를 보장하지 않는다. 프로세스 영구 생존도 보장하지 않는다.

공식 참고: [서비스 유형](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use), [WindowManager 플래그](https://developer.android.com/reference/android/view/WindowManager.LayoutParams), [AGP 호환성](https://developer.android.com/build/releases/agp-8-11-0-release-notes).

## Spike 1~3 통합 테스트 (0.2.0 / versionCode 2)

### 관측 경계

- **최근 외부 앱**: 시작 이후 `UsageStatsManager.queryEvents`의 최근 `ACTIVITY_RESUMED`(구버전 `MOVE_TO_FOREGROUND`)를 약 1초 간격으로 확인한다. GHOST 자체 전환은 최근 외부 앱을 덮어쓰지 않는다. 앱 이름을 OS가 공개하지 않으면 패키지명을 표시하며 `QUERY_ALL_PACKAGES`는 요청하지 않는다.
- 결과는 **최근 관측값과 시각**이지 항상 정확한 현재 앱이라는 보장은 아니다. 분할 화면, PiP, 잠금/절전, 제조사 이벤트 지연은 실기기에서 관측해야 한다. 딴짓/집중 상태로 해석하지 않는다.
- **Chrome**: 정식 `com.android.chrome`만 지원 대상으로 한다. 접근성 연결 자체는 관측을 시작하지 않는다. 기존 시작 버튼으로 켠 동안에만 Chrome 이벤트를 처리하고, 종료/재시작 사이에 지연된 이벤트는 실행 세대 검사로 차단한다.
- 주소창 `com.android.chrome:id/url_bar`가 보이고 편집 중이 아닐 때만 주소를 읽어 도메인으로 즉시 축소한다. 경로·포트·쿼리·fragment를 결과에 보관하지 않는다. 검색어·내부 주소·자격증명 포함 주소·해석 불가 주소는 미감지다. IPv6/단일 레이블 내부 호스트는 이 어댑터에서 미감지다.
- 제목은 Chrome이 공개한 최상위 WebView 문서 루트의 제목 메타데이터를 사용한다. **문서 자식(본문·링크·입력창·iframe)은 탐색하지 않는다.** 제목이 없으면 OS 창 제목을 별도 표시하며 페이지 제목으로 가장하지 않는다. 페이지 제목은 최대 256자다. 주소를 관측하지 못하면 이전 제목과 새 도메인을 섞지 않는다.
- Chrome 버전/화면 구조에 따라 주소창 숨김, 새 탭, 전체 화면에서 값이 없을 수 있다. 2026-09-23 검증에서 **새 탭·주소창 편집 중·전체 화면은 감지되지 않았다**(#2). 커스텀 탭은 같은 날 `url_bar` 뷰 ID 직접 조회를 추가해 감지된다. 시크릿 모드도 감지되며, 제외 여부는 정책 판단이 남아 있다. 제목/주소의 원자적 동일 페이지 보장이나 모든 Chrome 버전 지원은 미검증이다. 광범위 본문 수집으로 우회하지 않는다.
- 앱이 관측 데이터를 파일/DB/환경설정/로그에 쓰거나 네트워크로 보내지 않는다. 종료 시 결과는 지워지지만 OS가 관리하는 접근성 허용 상태는 유지된다. 접근성을 켜 둬도 앱의 관측 코드는 정지 중 루트/주소/제목을 읽지 않는다.
- 사용 정보 권한 미허용/조회 실패나 접근성 연결 해제는 진단 상태로 표시하며 플로팅을 강제 종료하지 않는다. 오버레이 권한 철회/창 실패는 기존대로 전체 실행을 정리한다.

### 휴대폰에서 한 번에 확인할 순서

설정은 **종료 상태**에서 수행한다. 개인 계정/민감한 페이지 대신 공개 테스트 페이지만 사용한다. 아래의 시작·종료와 결과 확인은 **더보기 > 개발자 도구** 화면에서 한다.

1. 최신 APK 설치 → 온보딩에서(또는 건너뛴 뒤 개발자 도구에서) 표시 권한 허용 → 사용 정보 접근에서 Ghost 허용 → 접근성의 설치된 앱에서 **GHOST Chrome 감지 테스트** 허용 → Ghost 복귀. 허용만으로 유령/감지가 시작되지 않는지 확인한다.
2. **시작** → 유령 드래그/탭(“안녕?”)/바깥 터치를 확인한다. Chrome 등 외부 앱으로 갔다가 GHOST로 돌아와 최근 외부 앱 이름/패키지/시각을 확인한다.
3. Chrome에서 `https://example.com/?ghost_test=123#probe`를 연 뒤 GHOST 복귀 → 도메인은 `example.com`만 나오는지, 제목은 `Example Domain` 또는 명시된 창 제목인지 확인한다. URL 원문/쿼리/fragment가 표시되면 실패다.
4. Chrome에서 다른 공개 페이지로 전환 → 도메인/제목/시각 갱신 확인. 주소창 편집·새 탭·주소창 숨김에서도 이전 값을 현재값으로 오해하지 않도록 미감지/최근 시각을 확인한다. 개인정보가 담긴 UI dump/logcat 전체 덤프를 공유하지 않는다.
5. **종료** → 유령과 결과가 사라지는지 확인. Chrome을 사용해도 결과가 생기지 않고, 다시 시작했을 때 이전 기록이 복원되지 않아야 한다.
6. 종료 후 감지 권한을 하나씩 끄고 시작 → 해당 권한 필요 상태 표시, 플로팅은 유지. 오버레이 권한 철회는 전체 정리. 잠금/해제·회전 및 기존 Spike 1 인수 체크리스트도 함께 확인한다.

### 현재 증거와 미완료

- 통합 단위 테스트 **35개 통과**: 기존 플로팅 13개, 앱/실행 수명 6개, Chrome 정제/탐색 경계 10개, Chrome 연결/실행 수명 6개.
- `lintDebug`: 오류 0개, 경고 12개. 기존 업데이트/KTX/백업 권고와 `isAccessibilityTool`의 API 31 미만 무시 경고. 무관한 라이브러리 업그레이드는 하지 않았다.
- 앱/계측 테스트 APK 빌드 통과. 별도 타입 검사 도구 대신 Kotlin 컴파일을 수행했다.
- 2026-09-12 삼성 SM-G998N / Android 15에 **통합 APK 0.2.0(2) 설치**, `OverlayControllerDeviceTest` 2개 재통과. 플로팅과 탭 인사 말풍선을 실제 GHOST 화면에서도 확인했다.
- Chrome 152.0.7977.82에서 최근 외부 앱 `com.android.chrome` 관측 확인. GHOST로 복귀한 뒤에도 최근 외부 앱과 관측 시각 표시를 확인했다.
- 공개 테스트 페이지에서 **`example.com` + 창 제목 `Chrome: Example Domain`**, **`google.com` + 창 제목 `Chrome: Google`** 표시를 확인했다. example.com 테스트 URL의 `?ghost_test=123#probe`는 도메인 결과에 남지 않았다. 사용자도 Google 감지 결과 표시를 확인했다.
- 위 제목은 **창 제목 대체값**이며 문서 루트의 페이지 제목 직접 감지가 통과했다는 뜻은 아니다.
- 2026-09-23 삼성 실기기에서 Chrome 조건별 감지를 검증했고 **일부 조건에서 감지에 실패했다.** 일반 페이지와 시크릿 모드는 감지되지만 **새 탭·주소창 편집 중·전체 화면은 감지되지 않는다.** 같은 날 커스텀 탭은 `url_bar` 뷰 ID 직접 조회를 추가해 감지되도록 고쳤다(SM-G998N / Android 15 / Chrome 153에서 확인). 남은 항목은 #2에서 추적한다.
- **페이지 제목 직접 감지는 어떤 조건에서도 성공하지 못했다.** 일반 페이지에서도 창 제목(`Chrome: Example Domain`)으로 대체된다.
- 검증에 `uiautomator dump`를 쓰면 접근성 연결이 끊겨 관측이 멈춘다. 화면 캡처로 확인한다.
- 진단 중 `uiautomator dump`가 접근성 연결을 일시 중단하는 간섭을 확인했다. 해당 도구를 제외하고 공개 페이지를 다시 열어 GHOST의 실제 화면 캡처로 감지를 재확인했다. 접근성 검증에서 기본 UIAutomation의 서비스 억제 동작에 주의한다([공식 문서](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)).
- **남은 검증:** 여러 앱 전환, 통합 실행의 종료·재시작/권한 철회/잠금·회전 및 전체 UI·터치 인수 체크리스트. 기본 동작 확인과 전체 인수를 구분한다.
- 필수 `visual-verdict` 스킬은 미설치로 미실행. 확인한 GHOST 화면과 계측 로그는 로컬 검증 자료이며 개인 휴대폰 원본 자료는 저장소에 포함하지 않는다.

근거: [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager), [접근성 서비스](https://developer.android.com/guide/topics/ui/accessibility/service), [창 제목](https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo#getTitle()), [Chromium 주소창 ID](https://github.com/chromium/chromium/blob/main/chrome/android/java/res/layout/url_bar.xml), [Chromium 문서 루트 텍스트 처리](https://github.com/chromium/chromium/blob/main/content/browser/accessibility/browser_accessibility_android.cc). Chromium 내부 구조는 안정된 외부 계약이 아니므로 대상 기기 Chrome에서의 확인이 필요하다. 접근성 기능의 Play 정책 적합성은 별도 검토 대상이며 이 내부 스파이크의 빌드 성공이 배포 승인 근거는 아니다.
