package com.ghost.prototype.detection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ghost.prototype.GhostApplication

class ChromeAccessibilityService : AccessibilityService() {
    private val store get() = (application as GhostApplication).detectionState
    private val handler = Handler(Looper.getMainLooper())
    private val reader = ChromeMetadataReader()
    private var scheduled = false

    override fun onServiceConnected() { store.chromeConnected(true) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!store.state.value.running || event?.packageName?.toString() != CHROME_PACKAGE || scheduled) return
        val runId = store.runId
        scheduled = true
        handler.postDelayed({
            scheduled = false
            // A delayed event from a previous run must not read the next session's screen.
            if (store.state.value.running && store.runId == runId) observe()
        }, 250)
    }

    private fun observe() {
        try {
            val root = rootInActiveWindow ?: return
            try {
                if (root.packageName?.toString() != CHROME_PACKAGE) return
                val window = root.window
                val title = try { window?.title?.toString() } finally {
                    @Suppress("DEPRECATION")
                    window?.recycle()
                }
                val node = AndroidChromeNode(root) { chromeWindowRoots() }
                store.chromeObserved(reader.read(node, title, System.currentTimeMillis()))
            } finally {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        } catch (_: RuntimeException) {
            // No exception payload or screen metadata in logs.
            store.chromeUnavailable()
        }
    }

    /**
     * 커스텀 탭은 활성 창의 루트가 웹 콘텐츠라 주소창이 그 아래에 없다.
     * 주소창을 찾을 때만 Chrome의 다른 창 루트도 후보로 넘긴다. 본문은 지나가지 않는다.
     */
    private fun chromeWindowRoots(): List<AccessibilityNodeInfo> = try {
        windows.mapNotNull { it.root }.filter { it.packageName?.toString() == CHROME_PACKAGE }
    } catch (_: RuntimeException) {
        emptyList()
    }

    override fun onInterrupt() {
        clearPending()
        store.chromeUnavailable()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearPending()
        store.chromeConnected(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clearPending()
        store.chromeConnected(false)
        super.onDestroy()
    }

    private fun clearPending() {
        handler.removeCallbacksAndMessages(null)
        scheduled = false
    }
}

private class AndroidChromeNode(
    private val node: AccessibilityNodeInfo,
    private val otherRoots: () -> List<AccessibilityNodeInfo> = { emptyList() },
) : ChromeNode {
    override val packageName get() = node.packageName?.toString()
    override val viewId get() = node.viewIdResourceName
    override val className get() = node.className?.toString()
    override val visible get() = node.isVisibleToUser
    override val focused get() = node.isFocused
    override val password get() = node.isPassword
    override val editable get() = node.isEditable
    override val text get() = node.text?.toString()
    override val description get() = node.contentDescription?.toString()
    override val childCount get() = node.childCount
    override fun child(index: Int): ChromeNode? = node.getChild(index)?.let(::AndroidChromeNode)
    override fun findByViewId(viewId: String): List<ChromeNode> =
        (node.findAccessibilityNodeInfosByViewId(viewId).orEmpty() +
            otherRoots().flatMap { it.findAccessibilityNodeInfosByViewId(viewId).orEmpty() })
            .take(4).map { AndroidChromeNode(it) }
    @Suppress("DEPRECATION")
    override fun release() { node.recycle() }
}
