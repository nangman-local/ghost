package com.ghost.prototype.detection

import org.junit.Assert.*
import org.junit.Test

class ChromeMetadataTest {
    @Test fun domainDropsPathQueryFragmentAndPort() {
        assertEquals("example.com", chromeDomain("https://Example.com:443/private?q=secret#token"))
        assertEquals("www.example.com", chromeDomain("www.example.com/path?q=secret"))
    }

    @Test fun unsafeOrNonWebAddressesAreNotDomains() {
        listOf(null, "", "search words", "chrome://newtab", "about:blank", "javascript:alert(1)",
            "https://user:secret@example.com", "https://example.com\\evil", "https://exa%6dple.com",
            "https://", "not-a-domain", "https://example..com", "https://example.com\n/secret",
            "https://example.com:invalid", "https://example.com:65536", "https://example.com:",
        ).forEach { assertNull(it, chromeDomain(it)) }
    }

    @Test fun internationalDomainIsNormalizedWithoutItsQuery() {
        assertEquals("xn--bcher-kva.de", chromeDomain("https://bücher.de/search?q=secret"))
    }

    @Test fun titlesAreBoundedAndUrlFallbackIsNotExposed() {
        assertEquals("Hello World", chromeTitle(" Hello\nWorld "))
        assertEquals(256, chromeTitle("a".repeat(300))?.length)
        assertNull(chromeTitle("https://example.com?q=secret"))
        assertNull(chromeTitle("\u200b"))
    }

    @Test fun readerNeverReadsBodyOrOrdinaryUiText() {
        val body = Node(secret = true)
        val document = Node(className = "android.webkit.WebView", value = "Example Domain", children = listOf(body))
        val ordinaryUi = Node(secret = true)
        val observation = ChromeMetadataReader().read(Node(children = listOf(address(), ordinaryUi, document)), "Chrome", 100)
        assertEquals("example.com", observation.domain)
        assertEquals("Example Domain", observation.title)
        assertEquals(TitleSource.PAGE, observation.titleSource)
        assertEquals(0, document.childReads)
        assertEquals(0, body.textReads)
        assertTrue(document.released)
        assertTrue(ordinaryUi.released)
    }

    @Test fun editingAddressDoesNotReadInputOrKeepOldTitle() {
        val address = Node(viewId = "$CHROME_PACKAGE:id/url_bar", focused = true, secret = true)
        val observation = ChromeMetadataReader().read(Node(children = listOf(address)), "Previous Page", 100)
        assertNull(observation.domain)
        assertNull(observation.title)
        assertEquals(0, address.textReads)
    }

    @Test fun missingDocumentUsesExplicitWindowSourceNotInventedPageTitle() {
        val observation = ChromeMetadataReader().read(Node(children = listOf(address())), "Chrome", 100)
        assertEquals("Chrome", observation.title)
        assertEquals(TitleSource.WINDOW, observation.titleSource)
    }

    @Test fun missingAddressNeverPairsWindowTitleWithPreviousDomain() {
        assertNull(ChromeMetadataReader().read(Node(), "Old page", 100).domain)
        assertNull(ChromeMetadataReader().read(Node(), "Old page", 100).title)
    }

    @Test fun otherPackagesHiddenAndPasswordNodesAreNotRead() {
        val nodes = listOf(Node(packageName = "other", secret = true), Node(visible = false, secret = true),
            Node(password = true, secret = true), Node(editable = true, secret = true))
        ChromeMetadataReader().read(Node(children = nodes), null, 100)
        nodes.forEach { assertEquals(0, it.textReads) }
    }

    @Test fun traversalIsBounded() {
        val root = Node(children = List(200) { Node() })
        ChromeMetadataReader().read(root, null, 100)
        assertTrue(root.childReads <= 127)
    }

    private fun address() = Node(viewId = "$CHROME_PACKAGE:id/url_bar", value = "example.com/path?q=secret")

    private class Node(
        override val packageName: String? = CHROME_PACKAGE,
        override val viewId: String? = null,
        override val className: String? = "android.view.ViewGroup",
        override val visible: Boolean = true,
        override val focused: Boolean = false,
        override val password: Boolean = false,
        override val editable: Boolean = false,
        val value: String? = null,
        val children: List<Node> = emptyList(),
        val secret: Boolean = false,
    ) : ChromeNode {
        var textReads = 0
        var childReads = 0
        var released = false
        override val text: String? get() {
            textReads++
            check(!secret) { "Forbidden text access" }
            return value
        }
        override val description: String? get() {
            check(!secret) { "Forbidden description access" }
            return null
        }
        override val childCount get() = children.size
        override fun child(index: Int): ChromeNode { childReads++; return children[index] }
        override fun release() { released = true }
    }
}
