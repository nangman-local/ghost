package com.ghost.prototype.contract

import kotlinx.coroutines.flow.StateFlow

/** `shared/states.md`의 세션 상태 `state`. 이름을 그대로 쓴다. */
enum class SessionState { WAITING, FOCUS, DISTRACT }

data class FocusSnapshot(
    val taskId: String? = null,
    val state: SessionState = SessionState.WAITING,
    /** `shared/states.md`의 `interventionLevel` (0~3). */
    val interventionLevel: Int = 0,
    val floatingVisible: Boolean = false,
    val error: String? = null,
)

/**
 * UI ↔ 코어(세션·상태머신·플로팅) 경계. 구현은 Android 코어 오너가 한다.
 * UI는 이 인터페이스만 보고, 서비스·오버레이·상태머신을 직접 참조하지 않는다.
 */
interface FocusController {
    val state: StateFlow<FocusSnapshot>

    /** 할 일을 시작한다 (`WAITING → FOCUS`). 실패는 [FocusSnapshot.error]로 알린다. */
    fun startTask(taskId: String)

    /** 세션을 끝낸다 (`* → WAITING`). 할 일 자체는 남는다. */
    fun stop()
}
