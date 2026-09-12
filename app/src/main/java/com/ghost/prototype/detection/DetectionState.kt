package com.ghost.prototype.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ObservationStatus { STOPPED, PERMISSION_REQUIRED, OBSERVING, UNAVAILABLE }

data class AppObservation(val packageName: String, val label: String, val observedAt: Long)

data class DetectionState(
    val running: Boolean = false,
    val startedAt: Long = 0,
    val usageStatus: ObservationStatus = ObservationStatus.STOPPED,
    val recentApp: AppObservation? = null,
    val chromeConnected: Boolean = false,
    val chromeUnavailable: Boolean = false,
    val recentChrome: ChromeObservation? = null,
)

/** Process-local diagnostic data, not the server's focus session or a usage history. */
class DetectionStateStore(private val ownPackage: String) {
    private val mutableState = MutableStateFlow(DetectionState())
    val state = mutableState.asStateFlow()
    var runId: Long = 0
        private set

    fun start(now: Long) {
        if (!state.value.running) {
            runId++
            mutableState.value = DetectionState(running = true, startedAt = now, chromeConnected = state.value.chromeConnected)
        }
    }

    fun stop() {
        runId++
        mutableState.value = DetectionState(chromeConnected = state.value.chromeConnected)
    }

    fun chromeConnected(connected: Boolean) {
        mutableState.update {
            it.copy(chromeConnected = connected, chromeUnavailable = false,
                recentChrome = if (connected) it.recentChrome else null)
        }
    }

    fun chromeUnavailable() {
        mutableState.update { if (it.running) it.copy(chromeUnavailable = true, recentChrome = null) else it }
    }

    fun chromeObserved(observation: ChromeObservation) {
        mutableState.update {
            if (!it.running || !it.chromeConnected || observation.observedAt < it.startedAt ||
                observation.observedAt < (it.recentChrome?.observedAt ?: it.startedAt)
            ) it else it.copy(recentChrome = observation, chromeUnavailable = false)
        }
    }

    fun usageStatus(status: ObservationStatus) {
        mutableState.update { if (it.running) it.copy(usageStatus = status) else it }
    }

    fun appObserved(observation: AppObservation) {
        mutableState.update {
            if (!it.running || observation.packageName == ownPackage || observation.packageName.isBlank() ||
                observation.observedAt < it.startedAt ||
                observation.observedAt < (it.recentApp?.observedAt ?: it.startedAt)
            ) it else it.copy(recentApp = observation)
        }
    }
}
