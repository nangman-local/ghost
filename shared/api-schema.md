# API 스키마 (단일 진실의 원천)

상태 이름은 [`states.md`](./states.md)를 따른다. 변경 시 팀 공유 채널에 공지한다.

- Base URL: 환경변수로 주입. 클라이언트 코드에 하드코딩하지 않는다.
- 인증: `Authorization: Bearer <accessToken>`
- 시각: ISO 8601 + 오프셋 (`2026-10-01T20:15:30+09:00`)
- 에러: `{ "error": { "code": "STRING_CODE", "message": "사람이 읽을 설명" } }`

## 세션

### `GET /sessions/current`

현재 활성 세션. **서버 세션이 진실의 원천이다.**

```json
{
  "sessionId": "s_123",
  "userId": "u_1",
  "task": { "id": "t_9", "title": "자료구조 과제", "source": "CALENDAR" },
  "state": "FOCUS",
  "activeDevice": "PC",
  "devices": [
    { "type": "PC", "lastHeartbeat": "2026-10-01T20:15:30+09:00" },
    { "type": "ANDROID", "lastHeartbeat": "2026-10-01T20:15:12+09:00" }
  ],
  "interventionLevel": 0,
  "distractSeconds": 0
}
```

`task.source`: `CALENDAR` · `NOTION` · `MANUAL`

### `POST /sessions` — 세션 시작

요청 `{ "taskId": "t_9", "device": "ANDROID" }` → 응답: 위 세션 객체

### `POST /sessions/{id}/end` — 세션 종료

### `POST /sessions/{id}/heartbeat` — 하트비트 (30초 주기)

```json
{ "device": "ANDROID", "app": "com.instagram.android", "domain": null, "title": null }
```

응답: 위 세션 객체. 끊기면 해당 기기를 비활성으로 처리한다.

**`domain`과 `title`만 보낸다. URL 쿼리스트링과 화면 텍스트는 보내지 않는다.**

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
