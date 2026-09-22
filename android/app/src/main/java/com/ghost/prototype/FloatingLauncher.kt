package com.ghost.prototype

import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.ghost.prototype.floating.FloatingService

/** 플로팅 시작·종료 명령. `MainViewModel`에서 옮겨 왔으며 동작은 같다. */
class FloatingLauncher(private val app: GhostApplication) {
    fun start() {
        if (app.floatingState.state.value.visible) return
        if (!Settings.canDrawOverlays(app)) {
            app.floatingState.failed(app.getString(R.string.error_permission))
            return
        }
        try {
            ContextCompat.startForegroundService(
                app, Intent(app, FloatingService::class.java).setAction(FloatingService.ACTION_SHOW),
            )
        } catch (_: RuntimeException) {
            app.floatingState.failed(app.getString(R.string.error_overlay))
        }
    }

    fun stop() {
        app.detectionState.stop()
        app.stopService(Intent(app, FloatingService::class.java))
    }
}
