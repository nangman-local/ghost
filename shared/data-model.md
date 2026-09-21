# 서버 데이터 모델 초안

## User
- id
- authSubject

## Task
- id
- userId
- title
- source

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
사용자 교정(`POST /tasks/{id}/allowlist`)은 `allowDomains`에 즉시 반영한다.

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
