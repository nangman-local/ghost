package com.ghost.prototype.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * 화면이 켜져 있는지만 본다(#70). 잠금 여부는 보지 않는다 — 잠금 화면에서도
 * 화면 자체는 켜져 있을 수 있고, 그 경우까지 딴짓 누적을 막을 근거는 없다.
 *
 * `ACTION_SCREEN_ON`/`OFF`는 등록 시점의 현재 상태를 주지 않으므로, 생성 시
 * `PowerManager.isInteractive`로 초기값을 읽는다.
 */
class ScreenStateMonitor(private val context: Context) {
    @Volatile
    private var screenOn: Boolean = context.getSystemService(PowerManager::class.java).isInteractive

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            screenOn = intent.action == Intent.ACTION_SCREEN_ON
        }
    }

    /** [GhostFocusController]가 매 틱 읽는다. 등록 전에는 항상 true다. */
    fun isScreenOn(): Boolean = screenOn

    /** 앱 프로세스 생존 기간 내내 한 번만 등록한다. 해제하지 않는다(프로세스와 함께 사라짐). */
    fun register() {
        // SCREEN_ON/OFF는 시스템만 보내는 보호된 브로드캐스트라 다른 앱에 노출할 필요가 없다.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
