package com.ghost.prototype

import android.app.Application
import com.ghost.prototype.contract.FocusController
import com.ghost.prototype.contract.PermissionStatus
import com.ghost.prototype.contract.TaskRepository
import com.ghost.prototype.fake.FakeFocusController
import com.ghost.prototype.fake.FakeTaskRepository
import com.ghost.prototype.floating.FloatingStateStore
import com.ghost.prototype.detection.DetectionStateStore
import com.ghost.prototype.permission.AndroidPermissionStatus
import kotlinx.coroutines.MainScope

class GhostApplication : Application() {
    // Process-local only: no service, window, or position is restored after process death.
    val floatingState = FloatingStateStore()
    val detectionState by lazy { DetectionStateStore(packageName) }
    val floatingLauncher by lazy { FloatingLauncher(this) }

    // UI ↔ 코어 ↔ 서버 연결 지점. Fake를 실제 구현으로 바꿀 때는 여기만 고친다.
    private val appScope = MainScope()
    val focusController: FocusController by lazy { FakeFocusController(floatingLauncher, floatingState.state, appScope) }
    val taskRepository: TaskRepository by lazy { FakeTaskRepository() }
    val permissionStatus: PermissionStatus by lazy { AndroidPermissionStatus(this) }
}
