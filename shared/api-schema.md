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

MVP 1차 Task API는 `GET /tasks`, `POST /tasks`로 Task 기본 정보의 조회·생성만 구현한다.

- `source`: `CALENDAR` · `NOTION` · `MANUAL`. 직접 입력으로 생성한 Task는 서버가 `MANUAL`로 설정한다.
- 동일한 `title`의 Task 생성을 허용한다.
- userId는 Request Body나 Query Parameter로 받지 않으며, 아래 GET/POST 응답 Task에도 포함하지 않는다. 인증 적용 시 서버가 인증 정보에서 현재 사용자를 식별한다.
- `id`는 서버/DB에서 생성한다. 클라이언트는 ID를 생성하거나 형식을 해석하지 않는다. `t_9`는 예시일 뿐 생성 규칙이 아니다.
- Task 생성과 시작 전 Task 선택은 분리한다. 서버는 Task의 저장·조회를 담당하고, 시작 전 현재 Task 선택 상태는 Android에서 로컬로 관리한다. 세션이 시작된 이후 현재 수행 중인 Task는 Session의 `task`를 기준으로 한다.

### `GET /tasks` — 목록

현재 사용자의 Task 목록을 조회한다. Request Body는 없다.

응답: `200 OK`. wrapper 없이 Task 배열을 반환한다.

```json
[
  {
    "id": "t_9",
    "title": "자료구조 과제",
    "source": "MANUAL"
  }
]
```

Task가 없으면 `200 OK`로 빈 배열을 반환한다.

```json
[]
```

**TODO:** 목록 정렬 기준은 DB 모델과 함께 확정한다. 현재는 생성순·ID순·최신순 등 특정 순서를 보장하지 않는다.

### `POST /tasks` — 생성

현재 사용자가 직접 입력한 Task를 생성한다.

요청

```json
{
  "title": "자료구조 과제"
}
```

응답: `201 Created`. 생성된 Task 객체를 반환한다.

```json
{
  "id": "t_9",
  "title": "자료구조 과제",
  "source": "MANUAL"
}
```

- `title`은 필수다. 서버에서 앞뒤 공백을 제거한 값을 저장하고 응답한다. trim 후 빈 문자열이면 `400 Bad Request`를 반환한다. 오류 응답은 문서 상단의 공통 오류 형식을 따른다.
- `source`는 요청에서 받지 않으며 서버가 `MANUAL`로 설정한다.
- `POST /tasks` 자체는 현재 Task 선택 상태를 변경하지 않는다. Android는 성공 응답을 받은 뒤 생성된 Task를 로컬의 현재 선택 Task로 설정한다.
- MVP 1차에서는 TaskProfile 생성에 의존하지 않는다.

**TODO:** `title` 최대 길이 및 DB 제약은 DB 모델과 함께 확정한다. Task ID의 구체적인 생성 전략도 DB 담당자와 협의하며, 이번 계약에서는 정하지 않는다.

### 이후 구현할 기능

- `PATCH /tasks/{id}` — 수정. 구체적인 Request / Response 계약은 수정 기능 구현 전에 확정한다.
- `POST /tasks/{id}/allowlist` — 사용자 교정. `{ "app": "com.android.chrome", "domain": "docs.google.com" }` 을 즉시 허용 목록에 반영

### 할 일 프로필

MVP 1차에서는 TaskProfile의 LLM 생성·저장을 구현하지 않는다. POST /tasks는 Task 기본 정보만 생성하며 프로필 생성에 의존하지 않는다.

TaskProfile 생성 기능은 /judge 연동 단계에서 구현한다. 해당 기능이 도입된 이후에는 POST /tasks 시 LLM을 1회 호출해 프로필을 생성·저장하고, /judge 판단 시에는 저장된 프로필을 조회해 사용한다. 판단할 때마다 프로필 생성용 LLM을 호출하지 않는다.
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
