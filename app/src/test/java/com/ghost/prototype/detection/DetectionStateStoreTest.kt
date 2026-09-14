package com.ghost.prototype.detection

import org.junit.Assert.*
import org.junit.Test

class DetectionStateStoreTest {
    private val store = DetectionStateStore("ghost")
    private fun observed(name: String = "chrome", time: Long = 110) = AppObservation(name, name, time)

    @Test fun stoppedIgnoresObservationsAndStatus() {
        store.appObserved(observed())
        store.usageStatus(ObservationStatus.OBSERVING)
        assertEquals(DetectionState(), store.state.value)
    }

    @Test fun ownAppDoesNotOverwriteRecentExternalApp() {
        store.start(100)
        store.appObserved(observed())
        store.appObserved(observed("ghost", 120))
        assertEquals("chrome", store.state.value.recentApp?.packageName)
    }

    @Test fun historyBeforeStartAndOutOfOrderEventsAreIgnored() {
        store.start(100)
        store.appObserved(observed(time = 99))
        assertNull(store.state.value.recentApp)
        store.appObserved(observed("new", 120))
        store.appObserved(observed("old", 110))
        assertEquals("new", store.state.value.recentApp?.packageName)
    }

    @Test fun duplicateStartDoesNotResetObservation() {
        store.start(100)
        store.appObserved(observed())
        store.start(200)
        assertEquals(100L, store.state.value.startedAt)
        assertNotNull(store.state.value.recentApp)
    }

    @Test fun stopAndRestartClearDataAndRejectOldEvents() {
        store.start(100)
        store.appObserved(observed())
        store.stop()
        assertEquals(DetectionState(), store.state.value)
        store.start(200)
        store.appObserved(observed())
        assertNull(store.state.value.recentApp)
    }

    @Test fun permissionLossIsSeparateFromRecentObservation() {
        store.start(100)
        store.appObserved(observed())
        store.usageStatus(ObservationStatus.PERMISSION_REQUIRED)
        assertEquals(ObservationStatus.PERMISSION_REQUIRED, store.state.value.usageStatus)
        assertNotNull(store.state.value.recentApp)
    }
}
