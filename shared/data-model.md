# 서버 데이터 모델 초안

## User
- id
- authSubject

## Task
- id
- userId
- title
- source

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
- Task 1:N Session
- Session 1:N SessionDevice
- Session 1:N Event