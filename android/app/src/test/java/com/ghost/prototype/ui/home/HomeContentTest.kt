package com.ghost.prototype.ui.home

import com.ghost.prototype.contract.FocusStats
import com.ghost.prototype.contract.SessionState
import com.ghost.prototype.contract.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContentTest {
    private val task = Task("t1", "기말 과제 마감")

    @Test
    fun formatsTodayTotalAsHoursAndMinutes() {
        assertEquals("0:00", formatHoursMinutes(0))
        assertEquals("0:59", formatHoursMinutes(59 * 60 + 59))
        assertEquals("1:23", formatHoursMinutes(83 * 60))
        assertEquals("12:05", formatHoursMinutes(12 * 3600 + 5 * 60))
        assertEquals("0:00", formatHoursMinutes(-10))
    }

    @Test
    fun noCurrentTaskShowsInput() {
        val content = homeContent(null, null, SessionState.WAITING, emptyList(), FocusStats(), "과제")
        assertEquals(HomeContent.NoTask("과제"), content)
    }

    @Test
    fun currentTaskWithoutSessionIsReady() {
        val content = homeContent(task, null, SessionState.WAITING, listOf(task), FocusStats(), "")
        assertEquals(HomeContent.TaskReady(task), content)
    }

    @Test
    fun runningSessionShowsProgressAndTodayTotals() {
        val content = homeContent(task, "t1", SessionState.DISTRACT, listOf(task), FocusStats(683, 2063), "")
        assertTrue(content is HomeContent.InProgress)
        content as HomeContent.InProgress
        assertEquals("0:11", content.focusTime)
        assertEquals("0:34", content.distractTime)
    }

    @Test
    fun stoppedSessionReturnsToReady() {
        val content = homeContent(task, "t1", SessionState.WAITING, listOf(task), FocusStats(), "")
        assertEquals(HomeContent.TaskReady(task), content)
    }
}
