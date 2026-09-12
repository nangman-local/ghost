package com.ghost.prototype

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.ghost.prototype.floating.FloatingService

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GhostApplication
    val state = app.floatingState.state

    fun start() {
        if (state.value.visible) return
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
        app.stopService(Intent(app, FloatingService::class.java))
    }

    fun permissionSettingsUnavailable() {
        app.floatingState.reportError(app.getString(R.string.error_settings))
    }
}
