# API 스키마 (단일 진실의 원천)

상태 이름은 [`states.md`](./states.md)를 따른다. 변경 시 팀 공유 채널에 공지한다.

- Base URL: 환경변수로 주입. 클라이언트 코드에 하드코딩하지 않는다.
- 인증: `Authorization: Bearer <accessToken>`
- 시각: ISO 8601 + 오프셋 (`2026-10-01T20:15:30+09:00`)
- 에러: `{ "error": { "code": "STRING_CODE", "message": "사람이 읽을 설명" } }`
- 감지하지 못한 신호는 `null`로 보낸다. 빈 문자열이나 `"미감지"` 같은 문자열을 쓰지 않는다.

## 세션

W0 세션 API 입출력 초안. **서버 세션이 진실의 원천이다.**
하나의 할 일을 수행하는 Session에 PC와 Android가 동시에 연결될 수 있으며, `state`, `distractSeconds`, `interventionLevel`은 기기별로 관리한다. 단일 `activeDevice`는 사용하지 않는다.

### `GET /sessions/current`

현재 활성 세션 조회. 응답: 아래 전체 세션 객체.

```json
{
  "sessionId": "s_123",
  "userId": "u_1",
  "task": {
    "id": "t_9",
    "title": "자료구조 과제",
    "source": "CALENDAR"
  },
  "startedAt": "2026-10-01T20:00:00+09:00",
  "endedAt": null,
  "devices": [
    {
      "deviceId": "pc_1",
      "type": "PC",
      "state": "FOCUS",
      "distractSeconds": 0,
      "interventionLevel": 0,
      "stateChangedAt": "2026-10-01T20:00:00+09:00",
      "lastHeartbeatAt": "2026-10-01T20:15:30+09:00"
    },
    {
      "deviceId": "android_1",
      "type": "ANDROID",
      "state": "DISTRACT",
      "distractSeconds": 120,
      "interventionLevel": 0,
      "stateChangedAt": "2026-10-01T20:13:12+09:00",
      "lastHeartbeatAt": "2026-10-01T20:15:12+09:00"
    }
  ]
}
```

- `task.source`: `CALENDAR` · `NOTION` · `MANUAL`
- 기기 `state`: `WAITING` · `FOCUS` · `DISTRACT`. `interventionLevel`은 0~3이며 [`states.md`](./states.md)의 정의와 제약을 따른다.
- 기기 종류(`type`, 요청의 `deviceType`): `PC` · `ANDROID`
- `endedAt`이 `null`이면 진행 중, 시각이 있으면 종료다.

### `POST /sessions` — 세션 시작

할 일을 시작하고 최초 기기를 세션에 연결한다.

요청

```json
{
  "taskId": "t_9",
  "deviceId": "pc_1",
  "deviceType": "PC"
}
```

응답: 위 전체 세션 객체.

### `POST /sessions/{id}/heartbeat` — 하트비트 (30초 주기)

각 기기가 30초마다 자신의 현재 상태를 전송한다.

요청

```json
{
  "deviceId": "android_1",
  "state": "DISTRACT",
  "distractSeconds": 120,
  "interventionLevel": 0,
  "app": "com.instagram.android",
  "domain": null,
  "title": null
}
```

응답: 위 전체 세션 객체.

`stateChangedAt`과 `lastHeartbeatAt`은 서버 기준 시각으로 관리한다.

`app` 식별자는 전송할 수 있다. 브라우징 정보는 **`domain`과 `title`만 전송하며, URL 쿼리스트링과 화면 텍스트는 보내지 않는다.**

### `POST /sessions/{id}/end` — 세션 종료

세션을 종료하고 `endedAt`을 설정한다. 응답: 위 전체 세션 객체.

## 할 일

- `GET /tasks` — 목록
- `POST /tasks` — 생성. 등록 시 LLM 1회 호출로 "할 일 프로필"을 생성한다 (실시간 아님)
- `PATCH /tasks/{id}` — 수정
- `POST /tasks/{id}/allowlist` — 사용자 교정. `{ "app": "com.android.chrome", "domain": "docs.google.com" }` 을 즉시 허용 목록에 반영

### 할 일 프로필

`POST /tasks` 시 LLM을 1회 호출해 생성하고 저장한다. 판단할 때마다 호출하지 않는다.
`/judge`의 판단 기준이자, 로컬 규칙 판단의 2차 기준으로도 쓴다.

```json
{
  "taskId": "t_9",
  "allowDomains": ["docs.spring.io", "stackoverflow.com", "github.com"],
  "relatedKeywords": ["spring", "security", "jpa", "gradle", "java"],
  "blockCategories": ["SHORT_FORM", "GAME", "SOCIAL"],
  "youtubePolicy": "KEYWORD_ONLY"
}
```

- `allowDomains`: 이 할 일 동안 `FOCUS`로 간주할 도메인. [`distract-rules.json`](./distract-rules.json)의 전역 분류보다 우선한다.
- `relatedKeywords`: 제목 매칭용. `/judge`가 관련성 판단에 사용한다.
- `blockCategories`: 이 할 일 동안 `DISTRACT`로 간주할 카테고리.
- `youtubePolicy`: `KEYWORD_ONLY`(제목에 관련 키워드가 있을 때만 허용) · `ALLOW` · `BLOCK`
- 사용자 교정(`POST /tasks/{id}/allowlist`)은 해당 할 일의 `allowDomains`에 즉시 반영한다.

## 딴짓 판단 `POST /judge`

로컬 규칙이 `AMBIGUOUS`일 때만 호출한다. 호출 조건(유지 시간)은 [`distract-rules.json`](./distract-rules.json)의 `minHoldSeconds`를 따른다. 이 값을 다른 곳에 중복해서 적지 않는다.

요청

```json
{
  "sessionId": "s_123",
  "taskTitle": "자료구조 과제",
  "app": "chrome.exe",
  "domain": "youtube.com",
  "title": "퀵정렬 알고리즘 강의"
}
```

- 할 일 프로필은 서버가 `sessionId`로 조회해 사용한다. 클라이언트가 보내지 않는다.
- 감지하지 못한 신호는 `null`로 보낸다. Windows는 브라우저 URL을 얻을 수 없어 `domain`이 항상 `null`이다(`get-windows`의 url은 macOS 전용).

응답

```json
{ "label": "FOCUS", "confidence": 0.87, "cached": false }
```

`label`: `FOCUS` · `DISTRACT` · `AMBIGUOUS` — `confidence`: `0.0`~`1.0`

### 신호별 confidence 상한

서버가 보장한다. 신호가 부족할수록 단정하지 않는다.

| 가용 신호 | confidence 상한 |
| --- | --- |
| `domain` + `title` | 제한 없음 |
| `title`만 | 0.7 |
| `app`만 | `/judge`를 호출하지 않는다 (`AMBIGUOUS` 유지) |

개입 3단계 허용 임계값과 맞물려 동작한다. 임계값 정의는 [`states.md`](./states.md)를 따른다.

### 캐시

키는 `(taskId, app, domain, title)`이다. 같은 할 일이라도 다른 할 일에는 재사용하지 않는다.
캐시에서 응답하면 `cached: true`를 반환한다.

### 실패 시 동작

두 경우를 구분한다.

- **LLM 호출 실패 (서버는 정상):** `200`으로 `{ "label": "AMBIGUOUS", "confidence": 0.0, "cached": false }`를 반환한다. 5xx로 클라이언트를 막지 않는다.
- **서버 자체가 응답하지 않거나 오프라인:** 클라이언트는 로컬 규칙만으로 판단한다.

**두 경우 모두 결과가 `AMBIGUOUS`면 개입하지 않는다.** 개입하지 않는 것이 올바른 실패 동작이다.

## 이벤트 `POST /events`

정의는 [`events.md`](./events.md) 참고. 배열로 배치 전송한다.

## WebSocket `/ws`

PC 클라이언트용. 서버 → 클라이언트 푸시.

```json
{ "type": "SESSION_UPDATED", "payload": { } }
{ "type": "INTERVENTION", "payload": { "level": 2, "message": "자료구조 과제 하던 중이었어" } }
```

진행 중인 세션에 다른 기기가 최초 연결되는 방식은 구현 전 합의한다.
