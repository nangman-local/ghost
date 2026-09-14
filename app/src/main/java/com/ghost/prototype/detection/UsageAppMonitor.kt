package com.ghost.prototype.detection

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper

/** Owned by the visible foreground service. Never queries usage from before Start. */
class UsageAppMonitor(private val context: Context, private val store: DetectionStateStore) {
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var cursor = 0L
    private val poll = object : Runnable {
        override fun run() {
            if (!active || !store.state.value.running) return
            sample()
            if (active) handler.postDelayed(this, POLL_MILLIS)
        }
    }

    fun start() {
        if (active) return
        active = true
        cursor = store.state.value.startedAt
        handler.post(poll)
    }

    fun stop() {
        active = false
        handler.removeCallbacks(poll)
        store.stop()
    }

    private fun sample() {
        val now = System.currentTimeMillis()
        val begin = maxOf(store.state.value.startedAt, cursor - POLL_MILLIS)
        cursor = now
        if (!DetectionPermissions.usageAccess(context)) {
            store.usageStatus(ObservationStatus.PERMISSION_REQUIRED)
            return
        }
        if (begin >= now) return
        try {
            val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(begin, now)
            if (events == null) {
                store.usageStatus(ObservationStatus.UNAVAILABLE)
                return
            }
            store.usageStatus(ObservationStatus.OBSERVING)
            val event = UsageEvents.Event()
            var latest: AppObservation? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // MOVE_TO_FOREGROUND has the same value as ACTIVITY_RESUMED (API 29+).
                @Suppress("DEPRECATION")
                val resumed = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                val name = event.packageName
                if (resumed && !name.isNullOrBlank() && name != context.packageName &&
                    event.timeStamp >= (latest?.observedAt ?: begin)
                ) latest = AppObservation(name, name, event.timeStamp)
            }
            latest?.let { store.appObserved(it.copy(label = appLabel(it.packageName))) }
        } catch (_: RuntimeException) {
            // Never log observed packages or screen data. Missing observations are not judgments.
            store.usageStatus(ObservationStatus.UNAVAILABLE)
        }
    }

    private fun appLabel(name: String): String = try {
        val info = context.packageManager.getApplicationInfo(name, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        name // Package visibility can hide labels; do not request QUERY_ALL_PACKAGES.
    }

    companion object { private const val POLL_MILLIS = 1_000L }
}
