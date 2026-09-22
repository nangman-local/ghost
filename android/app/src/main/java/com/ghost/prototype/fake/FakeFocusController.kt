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

    override val state: StateFlow<FocusSnapshot> = combine(session, floating) { taskId, f ->
        FocusSnapshot(
            taskId = taskId,
            state = if (taskId == null) SessionState.WAITING else SessionState.FOCUS,
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
