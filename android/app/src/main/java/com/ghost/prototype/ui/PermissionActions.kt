package com.ghost.prototype.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

// MainActivity에서 옮겨 왔다. 설정 화면을 열 수 없으면 onUnavailable로 알리고 재시도하지 않는다.

fun Context.openOverlaySettings(onUnavailable: () -> Unit) =
    openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()), onUnavailable)

fun Context.openUsageSettings(onUnavailable: () -> Unit) =
    openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), onUnavailable)

fun Context.openAccessibilitySettings(onUnavailable: () -> Unit) =
    openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), onUnavailable)

/** 알림 팝업을 더 띄울 수 없을 때(두 번 거부) 쓰는 대체 경로. */
fun Context.openAppNotificationSettings(onUnavailable: () -> Unit) =
    openSettings(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        onUnavailable,
    )

private fun Context.openSettings(intent: Intent, onUnavailable: () -> Unit) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    }
}
