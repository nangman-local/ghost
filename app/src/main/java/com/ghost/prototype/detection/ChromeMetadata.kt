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
        // Do not pair a title with an unknown/being-edited address or preserve a previous page.
        val title = if (domain != null) pageTitle ?: chromeTitle(windowTitle) else null
        val source = if (title == null) null else if (pageTitle != null) TitleSource.PAGE else TitleSource.WINDOW
        return ChromeObservation(domain, title, source, now)
    }
}
