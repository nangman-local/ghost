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
    fun `세션 누적이 오늘 누적보다 크면 세션 값을 보여준다`() {
        // 서버가 오늘 누적을 안 내려주는 동안(#14) 화면에 0이 박혀 있지 않게 한다(#71).
        val content = homeContent(
            task, "t1", SessionState.DISTRACT, listOf(task), FocusStats(0, 0), "",
            sessionDistractSeconds = 754,
        )
        content as HomeContent.InProgress
        assertEquals("0:12", content.distractTime)
    }

    @Test
    fun `오늘 누적이 더 크면 오늘 누적을 보여준다`() {
        // 오늘 이미 쌓인 시간이 이번 세션보다 많을 수 있다. 그때는 오늘 값이 맞다.
        val content = homeContent(
            task, "t1", SessionState.DISTRACT, listOf(task), FocusStats(0, 2063), "",
            sessionDistractSeconds = 60,
        )
        content as HomeContent.InProgress
        assertEquals("0:34", content.distractTime)
    }

    @Test
    fun stoppedSessionReturnsToReady() {
        val content = homeContent(task, "t1", SessionState.WAITING, listOf(task), FocusStats(), "")
        assertEquals(HomeContent.TaskReady(task), content)
    }
}
