package com.ghost.prototype.floating

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.ghost.prototype.GhostApplication
import com.ghost.prototype.MainActivity
import com.ghost.prototype.R
import com.ghost.prototype.detection.UsageAppMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FloatingService : Service() {
    private val state get() = (application as GhostApplication).floatingState
    private var overlay: OverlayController? = null
    private val detection get() = (application as GhostApplication).detectionState
    private val usageMonitor by lazy { UsageAppMonitor(this, detection) }
    private val focus get() = (application as GhostApplication).focusController
    /** 개입 레벨 구독(#77). 서비스가 사는 동안만 돈다. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appearanceJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    private val permissionListener = AppOpsManager.OnOpChangedListener { op, packageName ->
        if (op == AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW && packageName == this.packageName) {
            handler.post {
                if (!Settings.canDrawOverlays(this)) fail(R.string.error_permission)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(AppOpsManager::class.java).startWatchingMode(
            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, permissionListener,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_SHOW) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            // Promote immediately, including the race where permission was just revoked.
            val notification = notification()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            if (!Settings.canDrawOverlays(this)) {
                fail(R.string.error_permission)
            } else {
                val controller = overlay ?: OverlayController(this, state) {
                    fail(R.string.error_overlay)
                }.also { overlay = it }
                controller.show()
                if (state.state.value.visible) {
                    detection.start(System.currentTimeMillis())
                    usageMonitor.start()
                    observeInterventionLevel()
                }
            }
        } catch (error: RuntimeException) {
            Log.e("GhostFloating", "Unable to start overlay", error)
            fail(R.string.error_overlay)
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.repositionWithinScreen()
    }

    private fun fail(message: Int) {
        appearanceJob?.cancel()
        usageMonitor.stop()
        state.failed(getString(message))
        overlay?.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ghost)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    /**
     * 개입 레벨이 바뀌면 캐릭터 표현을 바꾼다(#77).
     *
     * 서비스가 오버레이 창을 소유하므로(`android/AGENTS.md`) 구독도 여기서 한다.
     * 레벨만 꺼내서 구독하므로 세션의 다른 값이 바뀌어도 다시 그리지 않는다.
     */
    private fun observeInterventionLevel() {
        appearanceJob?.cancel()
        appearanceJob = focus.state
            .map { it.interventionLevel }
            .distinctUntilChanged()
            .onEach { level -> overlay?.applyAppearance(CharacterAppearance.forLevel(level)) }
            .launchIn(serviceScope)
    }

    override fun onDestroy() {
        appearanceJob?.cancel()
        serviceScope.cancel()
        usageMonitor.stop()
        getSystemService(AppOpsManager::class.java).stopWatchingMode(permissionListener)
        handler.removeCallbacksAndMessages(null)
        overlay?.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SHOW = "com.ghost.prototype.SHOW"
        private const val CHANNEL_ID = "floating_character"
        private const val NOTIFICATION_ID = 1
    }
}
