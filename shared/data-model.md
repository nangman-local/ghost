# 서버 데이터 모델 초안

## User
- id
- authSubject

## Task

| 컬럼 | 타입 | 제약 |
| --- | --- | --- |
| `id` | `uuid` | PK |
| `userId` | `uuid` | FK → User, NOT NULL |
| `title` | `varchar(100)` | NOT NULL |
| `source` | `varchar(16)` | NOT NULL, `CALENDAR` · `NOTION` · `MANUAL` |
| `createdAt` | `timestamptz` | NOT NULL |
| `updatedAt` | `timestamptz` | NOT NULL |

**`id`는 UUID다.** 자동 증가 정수를 쓰지 않는다. 할 일은 기기에서 오프라인으로 만들어질 수 있고(#53 로컬 영속화), 나중에 서버로 올라올 때 ID를 다시 매기면 이미 그 ID를 참조한 로컬 기록이 어긋난다. `events.md`의 `eventId`도 같은 이유로 클라이언트가 UUID로 생성한다.
API 응답에서는 문자열로 내보낸다 (`api-schema.md` 예시의 `"t_9"` 형식).

**`title`은 저장 전에 정규화한다.** 앞뒤 공백을 제거하고 연속된 공백을 하나로 줄인다. 정규화 결과가 빈 문자열이면 `400`.
제목은 LLM 프롬프트 입력이자 `JudgeCache` 키의 일부다. `"자료구조  과제"`와 `"자료구조 과제"`가 서로 다른 할 일로 저장되면 프로필이 두 번 생성되고 LLM도 두 번 호출된다.

**목록 정렬은 `createdAt DESC`다.** 최근에 만든 할 일이 위로 온다.

**`source`는 MVP에서 `MANUAL`만 쓴다.** 캘린더 연동이 MVP 이후로 빠졌지만, 나중에 마이그레이션하지 않도록 컬럼은 미리 둔다.

**제목이 바뀌면 판단 근거를 버린다.** `PATCH /tasks/{id}`로 `title`이 실제로 바뀌면 해당 `taskId`의 `TaskProfile`과 `JudgeCache`를 삭제한다. 그러지 않으면 할 일을 고쳐도 예전 기준으로 계속 판단한다. 새 프로필은 다음 판단 요청 때 다시 생성한다.

## TaskProfile
- taskId
- allowDomains
- relatedKeywords
- blockCategories
- youtubePolicy
- createdAt

할 일 등록 시 LLM을 1회 호출해 생성한다. 판단할 때마다 호출하지 않는다.
형식은 api-schema.md의 "할 일 프로필"을 따른다.

`distract-rules.json`의 전역 분류 위에 얹히는 할 일별 기준이다.
사용자 교정(`POST /tasks/{id}/allowlist`)은 `allowDomains`에 즉시 반영하고, 해당 `taskId`의 `JudgeCache`를 삭제한다.

## Session
- id
- userId
- taskId
- startedAt
- endedAt

하나의 할 일에 대한 전체 집중 세션이다.

## SessionDevice
- sessionId
- deviceId
- deviceType
- state
- distractSeconds
- interventionLevel
- stateChangedAt
- lastHeartbeatAt

하나의 Session에 PC와 Android가 동시에 연결될 수 있다.
기기별 상태는 독립적으로 관리한다.

예:
- PC: FOCUS
- Android: DISTRACT

## JudgeCache
- taskId
- app
- domain
- title
- label
- confidence
- createdAt

`/judge`의 LLM 판단 결과를 캐시한다. 키는 `(taskId, app, domain, title)`이다.
같은 도메인·제목이라도 할 일이 다르면 판단이 달라지므로 `taskId`를 키에 포함한다.

`domain`과 `title`은 감지하지 못한 경우 `null`이며, `null`도 키의 일부로 취급한다.

할 일의 `title`이 바뀌거나 사용자 교정이 들어오면 해당 `taskId`의 캐시를 삭제한다.

## Event
- eventId
- sessionId
- userId
- device
- type
- at
- payload

이벤트 정의는 events.md를 따른다.

## 관계
- User 1:N Task
- User 1:N Session
- Task 1:1 TaskProfile
- Task 1:N JudgeCache
- Task 1:N Session
- Session 1:N SessionDevice
- Session 1:N Event

## 남은 것

Session · SessionDevice · Event · User 의 타입과 제약은 이어서 정리한다.
