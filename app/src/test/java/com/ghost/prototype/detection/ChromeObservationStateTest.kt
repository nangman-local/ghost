package com.ghost.prototype.detection

import org.junit.Assert.*
import org.junit.Test

class ChromeObservationStateTest {
    private val store = DetectionStateStore("ghost")
    private val observation = ChromeObservation("example.com", "Example", TitleSource.PAGE, 110)

    @Test fun enablingAccessibilityDoesNotStartObservation() {
        store.chromeConnected(true)
        store.chromeObserved(observation)
        assertFalse(store.state.value.running)
        assertNull(store.state.value.recentChrome)
    }

    @Test fun connectedRunningSessionAcceptsOnlyFreshObservations() {
        store.chromeConnected(true)
        store.start(100)
        store.chromeObserved(observation.copy(observedAt = 99))
        assertNull(store.state.value.recentChrome)
        store.chromeObserved(observation)
        assertEquals(observation, store.state.value.recentChrome)
        store.chromeObserved(observation.copy(title = "older", observedAt = 105))
        assertEquals(observation, store.state.value.recentChrome)
    }

    @Test fun stopClearsDataWithoutMisrepresentingSystemConnection() {
        store.chromeConnected(true)
        store.start(100)
        val runId = store.runId
        store.chromeObserved(observation)
        store.stop()
        store.chromeObserved(observation)
        assertNull(store.state.value.recentChrome)
        assertTrue(store.state.value.chromeConnected)
        store.start(200)
        assertNotEquals(runId, store.runId)
        assertNull(store.state.value.recentChrome)
    }

    @Test fun disconnectClearsChromeAndPreventsFurtherObservations() {
        store.chromeConnected(true)
        store.start(100)
        store.chromeObserved(observation)
        store.chromeConnected(false)
        store.chromeObserved(observation)
        assertNull(store.state.value.recentChrome)
        assertTrue(store.state.value.running)
    }

    @Test fun missingMetadataReplacesOldPageRatherThanKeepingFalseAssociation() {
        store.chromeConnected(true)
        store.start(100)
        store.chromeObserved(observation)
        store.chromeObserved(ChromeObservation(null, null, null, 120))
        assertNull(store.state.value.recentChrome?.domain)
        assertNull(store.state.value.recentChrome?.title)
    }

    @Test fun errorDoesNotEndOverlaySessionAndNextObservationRecovers() {
        store.chromeConnected(true)
        store.start(100)
        store.chromeObserved(observation)
        store.chromeUnavailable()
        assertTrue(store.state.value.running)
        assertTrue(store.state.value.chromeUnavailable)
        assertNull(store.state.value.recentChrome)
        store.chromeObserved(observation)
        assertFalse(store.state.value.chromeUnavailable)
    }
}
