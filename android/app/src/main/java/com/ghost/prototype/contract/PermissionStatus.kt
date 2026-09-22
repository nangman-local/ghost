package com.ghost.prototype.contract

enum class Permission { OVERLAY, USAGE_ACCESS, ACCESSIBILITY, NOTIFICATIONS }

/** 시스템 권한 조회. 호출 시점의 값을 돌려주며, 화면은 onResume마다 다시 읽는다. */
fun interface PermissionStatus {
    fun isGranted(permission: Permission): Boolean
}
