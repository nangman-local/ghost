package com.ghost.prototype.floating

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayBoundsTest {
    private val bounds = OverlayBounds(0, 24, 400, 776)

    @Test fun startsAtRightCenter() {
        assertEquals(OverlayPosition(304, 356), bounds.initial(80, 88, 16))
    }

    @Test fun clampsAllEdges() {
        assertEquals(OverlayPosition(0, 24), bounds.clamp(OverlayPosition(-500, -500), 80, 88))
        assertEquals(OverlayPosition(320, 688), bounds.clamp(OverlayPosition(900, 900), 80, 88))
    }

    @Test fun keepsValidDragPosition() {
        assertEquals(OverlayPosition(120, 240), bounds.clamp(OverlayPosition(120, 240), 80, 88))
    }

    @Test fun handlesWindowLargerThanAvailableArea() {
        assertEquals(OverlayPosition(10, 20), OverlayBounds(10, 20, 30, 40).initial(80, 88, 16))
    }

    @Test fun reclampsOnRotation() {
        val landscape = OverlayBounds(24, 0, 776, 400)
        assertEquals(OverlayPosition(300, 312), landscape.clamp(OverlayPosition(300, 650), 80, 88))
    }

    @Test fun expandedGreetingWindowStaysInsideScreen() {
        assertEquals(OverlayPosition(320, 650), bounds.clamp(OverlayPosition(900, 900), 80, 126))
        assertEquals(OverlayPosition(100, 24), bounds.clamp(OverlayPosition(100, -14), 80, 126))
    }
}
