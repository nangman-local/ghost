package com.ghost.prototype.fake

import com.ghost.prototype.contract.FocusController
import com.ghost.prototype.contract.FocusError
import com.ghost.prototype.contract.FocusSnapshot
import com.ghost.prototype.contract.SessionState
import com.ghost.prototype.floating.FloatingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 코어 오너의 실제 구현이 나오기 전까지 쓰는 임시 구현.
 * 세션 상태는 메모리에만 두고, 플로팅은 주입받은 명령(지금은 `FloatingLauncher`)으로 띄운다.
 * 서버·상태머신 연동은 없다.
 */
class FakeFocusController(
    private val startFloating: () -> Unit,
    private val stopFloating: () -> Unit,
    floating: StateFlow<FloatingState>,
    /** 플로팅 오류가 권한 때문인지 가르는 데 쓴다. 코어의 오류가 아직 문자열이라 종류를 여기서 판단한다. */
    private val overlayGranted: () -> Boolean,
    scope: CoroutineScope,
) : FocusController {
    private val session = MutableStateFlow<String?>(null)

    /**
     * UI 담당이 '나의 기록' 같은 화면을 만들 수 있도록 그럴듯한 값을 낸다(#71).
     * 실제 누적은 코어의 상태머신이 한다 — 여기서는 시간을 세지 않는다.
     *
     * `StateFlow`로 두는 이유: 단순 프로퍼티로 두면 값을 바꿔도 [state]가 다시 계산되지 않는다.
     */
    private val fakeDistract = MutableStateFlow(0)

    /** 화면을 만들 때 쓰는 값. 음수는 0으로 맞춘다. */
    var distractSeconds: Int
        get() = fakeDistract.value
        set(value) { fakeDistract.value = value.coerceAtLeast(0) }

    override val state: StateFlow<FocusSnapshot> =
        combine(session, floating, fakeDistract) { taskId, f, distract ->
        FocusSnapshot(
            taskId = taskId,
            state = if (taskId == null) SessionState.WAITING else SessionState.FOCUS,
            distractSeconds = if (taskId == null) 0 else distract,
            floatingVisible = f.visible,
            error = f.error?.let {
                if (overlayGranted()) FocusError.OVERLAY_ATTACH_FAILED else FocusError.OVERLAY_PERMISSION_MISSING
            },
        )
    }.stateIn(scope, SharingStarted.Eagerly, FocusSnapshot())

    override fun startTask(taskId: String) {
        session.value = taskId
        startFloating()
    }

    override fun stop() {
        session.value = null
        stopFloating()
    }
}
