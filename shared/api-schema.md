# API 스키마 (단일 진실의 원천)

상태 이름은 [`states.md`](./states.md)를 따른다. 변경 시 팀 공유 채널에 공지한다.

- Base URL: 환경변수로 주입. 클라이언트 코드에 하드코딩하지 않는다.
- 인증: `Authorization: Bearer <accessToken>`
- 시각: ISO 8601 + 오프셋 (`2026-10-01T20:15:30+09:00`)
- 에러: `{ "error": { "code": "STRING_CODE", "message": "사람이 읽을 설명" } }`

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
- `POST /tasks/{id}/allowlist` — 사용자 교정. `{ "app": "com.google.chrome", "domain": "docs.google.com" }` 을 즉시 허용 목록에 반영

## 딴짓 판단 `POST /judge`

로컬 규칙이 `AMBIGUOUS`일 때만 호출한다. 앱/제목이 15~30초 이상 유지될 때만.

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

응답

```json
{ "label": "FOCUS", "confidence": 0.87, "cached": false }
```

`label`: `FOCUS` · `DISTRACT` · `AMBIGUOUS` — `confidence`: `0.0`~`1.0`

**서버가 실패하거나 오프라인이면 클라이언트는 로컬 규칙만으로 판단하고, 모호하면 개입하지 않는다.**

## 이벤트 `POST /events`

정의는 [`events.md`](./events.md) 참고. 배열로 배치 전송한다.

## WebSocket `/ws`

PC 클라이언트용. 서버 → 클라이언트 푸시.

```json
{ "type": "SESSION_UPDATED", "payload": { } }
{ "type": "INTERVENTION", "payload": { "level": 2, "message": "자료구조 과제 하던 중이었어" } }
```

진행 중인 세션에 다른 기기가 최초 연결되는 방식은 구현 전 합의한다.