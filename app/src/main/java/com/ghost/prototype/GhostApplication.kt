package com.ghost.prototype

import android.app.Application
import com.ghost.prototype.floating.FloatingStateStore
import com.ghost.prototype.detection.DetectionStateStore

class GhostApplication : Application() {
    // Process-local only: no service, window, or position is restored after process death.
    val floatingState = FloatingStateStore()
    val detectionState by lazy { DetectionStateStore(packageName) }
}
