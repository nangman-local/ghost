package com.ghost.prototype.detection

import java.net.IDN
import java.net.URI
import java.util.Locale

const val CHROME_PACKAGE = "com.android.chrome"
enum class TitleSource { PAGE, WINDOW }
data class ChromeObservation(
    val domain: String?,
    val title: String?,
    val titleSource: TitleSource?,
    val observedAt: Long,
)

/** Raw addresses never enter application state. No path, credentials, port, query or fragment. */
fun chromeDomain(address: String?): String? {
    val value = address?.trim()?.takeIf { it.isNotEmpty() && it.length <= 8192 } ?: return null
    if (value.any { it.isWhitespace() || it.isISOControl() } || value.contains('\\')) return null
    return try {
        val uri = URI(if (value.contains("://")) value else "https://$value")
        if (uri.scheme?.lowercase(Locale.ROOT) !in listOf("http", "https")) return null
        val authority = uri.rawAuthority ?: return null
        if (authority.contains('@') || authority.contains('%')) return null
        if (authority.contains(':')) {
            val port = authority.substringAfter(':')
            if (port.isEmpty() || port.any { !it.isDigit() } || port.toIntOrNull() !in 0..65535) return null
        }
        val host = authority.substringBefore(':').trimEnd('.')
        val ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
        if (ascii.length > 253 || !ascii.contains('.') || ascii.split('.').any { it.isBlank() }) null else ascii
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.net.URISyntaxException) {
        null
    }
}

/** Only document-root or window title metadata is passed here, never arbitrary node text. */
fun chromeTitle(title: String?): String? = title?.trim()
    ?.takeIf { it.isNotEmpty() && !it.contains("://") }
    ?.replace(Regex("[\\p{Cntrl}\\p{Cf}]"), " ")?.trim()?.takeIf { it.isNotEmpty() }?.take(256)

/** Small testable boundary: traversal stops at the web document, never entering page content. */
interface ChromeNode {
    val packageName: String?
    val viewId: String?
    val className: String?
    val visible: Boolean
    val focused: Boolean
    val password: Boolean
    val editable: Boolean
    val text: String?
    val description: String?
    val childCount: Int
    fun child(index: Int): ChromeNode?
    fun release()

    /**
     * 화면 전체를 훑지 않고 해당 뷰 ID의 노드만 직접 찾는다.
     * 커스텀 탭처럼 주소창이 트리 탐색으로 잡히지 않는 화면에서 쓴다. 본문은 지나가지 않는다.
     */
    fun findByViewId(viewId: String): List<ChromeNode> = emptyList()
}

class ChromeMetadataReader {
    fun read(root: ChromeNode, windowTitle: String?, now: Long): ChromeObservation {
        var domain: String? = null
        var pageTitle: String? = null
        var remaining = 128
        fun visit(node: ChromeNode, depth: Int) {
            if (remaining-- <= 0 || depth > 16 || node.packageName != CHROME_PACKAGE || !node.visible) return
            if (node.className == "android.webkit.WebView") {
                if (!node.password && !node.editable) {
                    pageTitle = pageTitle ?: chromeTitle(node.text) ?: chromeTitle(node.description)
                }
                return // Do not read/traverse body, links, text inputs or subframes.
            }
            if (node.viewId == "$CHROME_PACKAGE:id/url_bar") {
                if (!node.focused && !node.password) domain = chromeDomain(node.text)
                return // Never capture address editing or suggestions.
            }
            if (node.editable || node.password) return
            for (index in 0 until node.childCount.coerceAtMost(128)) {
                if (remaining <= 0) break
                val child = node.child(index) ?: continue
                try { visit(child, depth + 1) } finally { child.release() }
            }
        }
        visit(root, 0)
        // 커스텀 탭 등 트리 탐색으로 주소창에 닿지 못하는 화면: 주소창 ID로만 직접 조회한다.
        if (domain == null) domain = urlBarByViewId(root)
        // Do not pair a title with an unknown/being-edited address or preserve a previous page.
        val title = if (domain != null) pageTitle ?: chromeTitle(windowTitle) else null
        val source = if (title == null) null else if (pageTitle != null) TitleSource.PAGE else TitleSource.WINDOW
        return ChromeObservation(domain, title, source, now)
    }

    /**
     * 주소창 노드만 직접 조회한다. 편집 중·비밀번호·다른 앱 노드는 읽지 않는다.
     * 커스텀 탭의 주소창은 화면에 보여도 `isVisibleToUser=false`로 보고되므로(실기기 확인)
     * 이 경로에서는 가시성을 조건에 넣지 않는다. 트리 탐색 경로는 종전대로 가시성을 지킨다.
     */
    private fun urlBarByViewId(root: ChromeNode): String? {
        val nodes = root.findByViewId("$CHROME_PACKAGE:id/url_bar")
        var found: String? = null
        nodes.forEach { node ->
            try {
                if (found == null && node.packageName == CHROME_PACKAGE &&
                    !node.focused && !node.password
                ) {
                    found = chromeDomain(node.text)
                }
            } finally {
                node.release()
            }
        }
        return found
    }
}
