# 이벤트 정의 (지표 계산용)

`POST /events`로 배치 전송한다. 상태 이름은 [`states.md`](./states.md)를 따른다.

## 공통 형식

```json
{
  "eventId": "e_uuid",
  "sessionId": "s_123",
  "userId": "u_1",
  "device": "ANDROID",
  "type": "DISTRACT_STARTED",
  "at": "2026-10-01T20:15:30+09:00",
  "payload": { }
}
```

`eventId`는 클라이언트가 UUID로 생성한다. 서버는 중복 `eventId`를 무시한다(오프라인 재전송 대비).

## 이벤트 타입

| type | 발생 시점 | payload |
| --- | --- | --- |
| `SESSION_STARTED` | 세션 시작 | `{ taskId, taskSource }` |
| `SESSION_ENDED` | 세션 종료 | `{ reason }` — `USER` · `TIMEOUT` |
| `FOCUS_STARTED` | `→ FOCUS` 전이 | `{ app }` |
| `DISTRACT_STARTED` | `→ DISTRACT` 전이 | `{ app, domain, decidedBy }` |
| `DISTRACT_ENDED` | `DISTRACT →` 이탈 | `{ durationSeconds }` |
| `INTERVENTION_SHOWN` | 개입 표시 | `{ level, decidedBy, confidence }` |
| `INTERVENTION_DISMISSED` | 사용자가 무시/닫음 | `{ level, afterSeconds }` |
| `RETURNED_TO_TASK` | 개입 후 `→ FOCUS` 복귀 | `{ level, afterSeconds }` |
| `USER_CORRECTED` | "이거 할 일 관련이야" 교정 | `{ app, domain, newLabel }` |
| `JUDGE_CALLED` | `/judge` 호출 | `{ label, confidence, cached, latencyMs }` |
| `JUDGE_FAILED` | `/judge` 실패·오프라인 | `{ reason }` |

`decidedBy`: `RULE` · `AI` · `USER`

## 핵심 지표

**복귀율** — 핵심 가설 검증 지표

```
RETURNED_TO_TASK / INTERVENTION_SHOWN
```

개입 단계(`level`)별로 나눠 본다. 3단계가 1·2단계보다 복귀율이 높은지, 그 대가로 `INTERVENTION_DISMISSED`가 늘지 않는지 함께 본다.

**복귀 소요 시간** — `RETURNED_TO_TASK.afterSeconds` 중앙값

**오탐율** — 낮을수록 좋다. 오탐이 미탐보다 훨씬 치명적이다.

```
USER_CORRECTED(newLabel=FOCUS) / DISTRACT_STARTED
```

**AI 의존도** — MVP가 규칙만으로 동작하는지 확인

```
JUDGE_CALLED / (DISTRACT_STARTED + FOCUS_STARTED)
```

**AI 캐시 적중률** — `JUDGE_CALLED(cached=true) / JUDGE_CALLED`

## 프라이버시

- `domain`과 `title`만 보낸다. **URL 쿼리스트링과 화면 텍스트는 보내지 않는다.**
- `payload`에 개인 식별 정보를 넣지 않는다.
