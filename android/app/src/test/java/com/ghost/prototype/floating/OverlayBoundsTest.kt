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

    // --- 2분 협상 고정 위치(#63) ---

    @Test fun negotiationSitsAtScreenCenter() {
        // 유령이 어디에 드래그돼 있었는지와 무관하게 항상 같은 자리다 — 이 함수는 현재
        // 위치를 인자로 받지 않는다. 화면 크기·창 크기만으로 정해진다.
        assertEquals(OverlayPosition(80, 296), bounds.centered(240, 208))
    }

    @Test fun negotiationClampsWhenWindowIsWiderThanScreen() {
        val narrow = OverlayBounds(0, 24, 200, 776)
        assertEquals(OverlayPosition(0, 296), narrow.centered(240, 208))
    }
}
