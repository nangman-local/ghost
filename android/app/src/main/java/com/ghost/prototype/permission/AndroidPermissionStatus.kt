package com.ghost.prototype.permission

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.ghost.prototype.contract.Permission
import com.ghost.prototype.contract.PermissionStatus
import com.ghost.prototype.detection.ChromeAccessibilityService
import com.ghost.prototype.detection.DetectionPermissions

/** 시스템 설정을 직접 읽는다. 결과를 캐시하지 않는다. */
class AndroidPermissionStatus(private val context: Context) : PermissionStatus {
    override fun isGranted(permission: Permission): Boolean = when (permission) {
        Permission.OVERLAY -> Settings.canDrawOverlays(context)
        Permission.USAGE_ACCESS -> DetectionPermissions.usageAccess(context)
        Permission.ACCESSIBILITY -> accessibilityEnabled()
        Permission.NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // 서비스 연결 여부(프로세스 메모리)가 아니라 시스템에 켜져 있는지를 본다.
    private fun accessibilityEnabled(): Boolean =
        context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val service = it.resolveInfo.serviceInfo
                service.packageName == context.packageName && service.name == ChromeAccessibilityService::class.java.name
            }
}
