package com.ghost.prototype

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GhostApplication
    val state = app.floatingState.state
    val detection = app.detectionState.state

    /** 세션 상태. 홈과 같은 `FocusController`를 본다(#52). */
    val focus = app.focusController.state

    /**
     * 개발자 도구의 시작. **홈의 '시작하기'와 같은 경로를 쓴다**(#52).
     *
     * 예전에는 `FloatingLauncher`를 직접 불러서 플로팅 창만 띄웠다. 그러면 세션이 시작되지 않아
     * 딴짓을 아무리 해도 누적되지 않고 개입도 일어나지 않는다 — 검증할 때 버그와 구분이 안 된다.
     *
     * 개발자 도구에는 선택된 할 일이 없으므로 진단용 세션 id를 쓴다.
     */
    fun start() = app.focusController.startTask(DIAGNOSTIC_TASK_ID)

    fun stop() = app.focusController.stop()

    fun permissionSettingsUnavailable() {
        app.floatingState.reportError(app.getString(R.string.error_settings))
    }

    companion object {
        /**
         * 개발자 도구로 시작한 세션의 할 일 id. 실제 할 일이 아니라 검증용이다.
         * 서버 연동(#14) 때는 이 세션을 서버로 올리지 않거나, 진단용임을 표시해야 한다.
         */
        const val DIAGNOSTIC_TASK_ID = "diagnostic"
    }
}
