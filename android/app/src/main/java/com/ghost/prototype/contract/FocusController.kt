package com.ghost.prototype.contract

import kotlinx.coroutines.flow.StateFlow

/** `shared/states.md`의 세션 상태 `state`. 이름을 그대로 쓴다. */
enum class SessionState { WAITING, FOCUS, DISTRACT }

/**
 * 세션·플로팅 실패의 종류. 화면 문구와 버튼은 UI가 종류별로 정한다.
 * 새 실패가 생기면 여기에 추가하고 공지한다(UI의 `when`이 빠진 처리를 컴파일 오류로 알려준다).
 */
enum class FocusError {
    /** 다른 앱 위 표시 권한이 없다. UI는 권한 설정으로 안내한다. */
    OVERLAY_PERMISSION_MISSING,

    /** 권한은 있는데 플로팅 창을 띄우지 못했다. 재시도는 사용자가 한다. */
    OVERLAY_ATTACH_FAILED,
}

data class FocusSnapshot(
    val taskId: String? = null,
    val state: SessionState = SessionState.WAITING,
    /** `shared/states.md`의 `interventionLevel` (0~3). */
    val interventionLevel: Int = 0,
    /**
     * 이번 세션의 누적 딴짓 시간(초). `shared/api-schema.md`의 `devices[].distractSeconds`와 같은 값이다.
     *
     * **연속이 아니라 합계다.** 딴짓 → 잠깐 복귀 → 딴짓을 반복해도 이어지고,
     * 유예 시간 이상 집중해야 0으로 돌아간다. 세션이 끝나면 0이다.
     */
    val distractSeconds: Int = 0,
    val floatingVisible: Boolean = false,
    val error: FocusError? = null,
)

/**
 * UI ↔ 코어(세션·상태머신·플로팅) 경계. 구현은 Android 코어 오너가 한다.
 * UI는 이 인터페이스만 보고, 서비스·오버레이·상태머신을 직접 참조하지 않는다.
 * 지켜야 할 동작은 테스트 `FocusControllerContract`에 적혀 있다.
 */
interface FocusController {
    val state: StateFlow<FocusSnapshot>

    /** 할 일을 시작한다 (`WAITING → FOCUS`). 실패는 [FocusSnapshot.error]로 알린다. */
    fun startTask(taskId: String)

    /** 세션을 끝낸다 (`* → WAITING`). 할 일 자체는 남는다. */
    fun stop()
}
