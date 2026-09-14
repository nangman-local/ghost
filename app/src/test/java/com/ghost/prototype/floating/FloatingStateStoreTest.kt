package com.ghost.prototype.floating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingStateStoreTest {
    @Test fun greetingAppearsOnlyWhileVisibleAndResetsOnStop() {
        val store = FloatingStateStore()
        store.greet()
        assertFalse(store.state.value.greetingVisible)
        store.shown(OverlayPosition(100, 200))
        assertFalse(store.state.value.greetingVisible)
        store.greet()
        store.greet()
        assertTrue(store.state.value.greetingVisible)
        store.moved(OverlayPosition(150, 250))
        assertTrue(store.state.value.greetingVisible)
        store.stopped()
        assertFalse(store.state.value.greetingVisible)
        store.shown(OverlayPosition(300, 350))
        assertFalse(store.state.value.greetingVisible)
    }

    @Test fun newProcessStartsHiddenWithoutPosition() {
        assertEquals(FloatingState(), FloatingStateStore().state.value)
    }

    @Test fun showAndMoveReflectTheWindow() {
        val store = FloatingStateStore()
        store.shown(OverlayPosition(300, 350))
        store.moved(OverlayPosition(100, 200))
        assertTrue(store.state.value.visible)
        assertEquals(OverlayPosition(100, 200), store.state.value.position)
    }

    @Test fun stopForgetsPositionAndIsIdempotent() {
        val store = FloatingStateStore()
        store.shown(OverlayPosition(100, 200))
        store.stopped()
        store.stopped()
        assertEquals(FloatingState(), store.state.value)
    }

    @Test fun lateMoveCannotReviveStoppedWindow() {
        val store = FloatingStateStore()
        store.moved(OverlayPosition(100, 200))
        assertEquals(FloatingState(), store.state.value)
    }

    @Test fun cleanupPreservesFailureUntilSuccessfulStart() {
        val store = FloatingStateStore()
        store.shown(OverlayPosition(100, 200))
        store.failed("permission revoked")
        store.stopped()
        assertFalse(store.state.value.visible)
        assertNull(store.state.value.position)
        assertEquals("permission revoked", store.state.value.error)
        store.shown(OverlayPosition(300, 350))
        assertNull(store.state.value.error)
        assertTrue(store.state.value.visible)
    }

    @Test fun settingsErrorDoesNotHideAnExistingWindow() {
        val store = FloatingStateStore()
        store.shown(OverlayPosition(100, 200))
        store.reportError("settings unavailable")
        assertTrue(store.state.value.visible)
        assertEquals(OverlayPosition(100, 200), store.state.value.position)
        assertEquals("settings unavailable", store.state.value.error)
    }
}
