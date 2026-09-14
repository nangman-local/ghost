# 상태 정의 (단일 진실의 원천)

Android · Desktop · Server가 공유하는 상태 이름. **이 파일의 문자열을 그대로 쓴다.**
변경 시 팀 공유 채널에 공지하고, 세 클라이언트를 동시에 갱신한다.

## 세션 상태 `state`

| 값 | 의미 |
| --- | --- |
| `WAITING` | 할 일 미시작 / 대기 |
| `FOCUS` | 할 일 관련 행동 중 |
| `DISTRACT` | 딴짓 중 |

전이: `WAITING → FOCUS` (할 일 시작) · `FOCUS ↔ DISTRACT` (판정 누적) · `* → WAITING` (세션 종료)

## 개입 단계 `interventionLevel`

| 값 | 이름 | 행동 |
| --- | --- | --- |
| `0` | 없음 | 캐릭터 평상시 상태 |
| `1` | 환기 | 캐릭터 애니메이션 변화, 시선 끌기 |
| `2` | 복귀 제안 | 말풍선으로 현재 할 일 제시 |
| `3` | 화면 개입 | 콘텐츠 일부 가리기 (가린 영역만 터치 차단) |

4단계(강제 잠금)는 MVP 범위에서 제외한다.

**3단계는 규칙으로 확정됐거나 AI confidence가 높을 때만 허용한다.** 오탐이 미탐보다 훨씬 치명적이다.

## 활성 기기 `activeDevice`

`PC` · `ANDROID`

## 캐릭터 상태 `characterState`

Rive 애니메이션 상태 이름. 보민(캐릭터)이 `.riv`에 동일한 이름으로 정의한다.

| 값 | 대응 |
| --- | --- |
| `IDLE` | `state=FOCUS` 또는 `WAITING`, `interventionLevel=0` |
| `ALERT` | `interventionLevel=1` |
| `TALK` | `interventionLevel=2` |
| `BLOCK` | `interventionLevel=3` |

## 판정 라벨 `label`

`/judge` 응답과 로컬 규칙 판단이 공유한다.

`FOCUS` · `DISTRACT` · `AMBIGUOUS`

`AMBIGUOUS`는 개입하지 않는다. 캐릭터가 사용자에게 직접 물어본다.
