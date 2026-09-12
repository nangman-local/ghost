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

class FloatingService : Service() {
    private val state get() = (application as GhostApplication).floatingState
    private var overlay: OverlayController? = null
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

    override fun onDestroy() {
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
