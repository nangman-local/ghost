package com.ghost.prototype.fake

import com.ghost.prototype.FloatingLauncher
import com.ghost.prototype.contract.FocusController
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
 * 세션 상태는 메모리에만 두고, 플로팅은 기존 [FloatingLauncher]로 띄운다. 서버·상태머신 연동은 없다.
 */
class FakeFocusController(
    private val launcher: FloatingLauncher,
    floating: StateFlow<FloatingState>,
    scope: CoroutineScope,
) : FocusController {
    private val session = MutableStateFlow<String?>(null)

    override val state: StateFlow<FocusSnapshot> = combine(session, floating) { taskId, f ->
        FocusSnapshot(
            taskId = taskId,
            state = if (taskId == null) SessionState.WAITING else SessionState.FOCUS,
            floatingVisible = f.visible,
            error = f.error,
        )
    }.stateIn(scope, SharingStarted.Eagerly, FocusSnapshot())

    override fun startTask(taskId: String) {
        session.value = taskId
        launcher.start()
    }

    override fun stop() {
        session.value = null
        launcher.stop()
    }
}
