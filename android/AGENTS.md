# android/ — Android 클라이언트

Kotlin + Jetpack Compose. 팀 공통 규칙은 루트 [`AGENTS.md`](../AGENTS.md), 공통 정의는 [`shared/`](../shared/)를 따른다.

담당: 정수(코어 오너, 크리티컬 패스) · 현기(서브)

**현재 상태:** Spike 1~3 통합 구현. 플로팅 + 최근 앱 + Chrome 도메인/제목 관측까지 실기기 확인 완료. 서버 연동은 미구현.

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
MainActivity / MainViewModel     권한 안내, 사용자 시작·종료 명령
GhostApplication                 프로세스 내부 상태 저장소
floating/FloatingService         서비스·알림·권한 감시 수명
floating/OverlayController       WindowManager 창 생성·드래그·제거
floating/OverlayBounds           Android 독립 좌표 로직 (단위 테스트 대상)
detection/UsageAppMonitor        최근 외부 앱 관측
detection/ChromeAccessibilityService  Chrome 이벤트 어댑터
detection/ChromeMetadata         도메인 정제 + 문서 루트 제목
```

- **서비스가 오버레이 창을 소유한다.** ViewModel/Activity에 창이나 Service 인스턴스를 저장하지 않는다.
- Android 독립 로직(좌표 계산, 도메인 정제, 상태 전이)은 순수 Kotlin으로 분리해 단위 테스트를 붙인다.

## 권한

사용 정보 접근(`UsageStatsManager`)과 접근성(`AccessibilityService`)은 **사용자가 시스템 설정에서 직접 허용한다.** 권한 없이도 플로팅은 동작해야 한다.

- 권한 철회/창 연결 실패 시 서비스를 정리하고 앱에 오류를 표시한다. **자동 재시도하지 않는다.**
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
크리티컬 패스(감지·오버레이·상태머신) 변경은 정수 확인 후 머지한다.
