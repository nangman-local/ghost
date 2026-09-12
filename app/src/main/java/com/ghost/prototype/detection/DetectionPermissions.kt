package com.ghost.prototype.detection

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

object DetectionPermissions {
    fun usageAccess(context: Context): Boolean = try {
        context.getSystemService(AppOpsManager::class.java).checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    } catch (_: SecurityException) {
        false
    }
}
