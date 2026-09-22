package com.ghost.prototype

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GhostApplication
    val state = app.floatingState.state
    val detection = app.detectionState.state

    fun start() = app.floatingLauncher.start()

    fun stop() = app.floatingLauncher.stop()

    fun permissionSettingsUnavailable() {
        app.floatingState.reportError(app.getString(R.string.error_settings))
    }
}
