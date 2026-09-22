package com.ghost.prototype.contract

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 모든 [FocusController] 구현이 지켜야 할 동작.
 * 구현 담당(코어 오너)은 이 클래스를 상속한 테스트를 만들어 자기 구현으로 돌린다. 예: `FakeFocusControllerTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class FocusControllerContract {
    /** 테스트할 구현을 만든다. 플로팅·서버 같은 외부 의존은 성공하는 가짜로 채운다. 코루틴은 [TestScope.backgroundScope]에서. */
    protected abstract fun TestScope.create(): FocusController

    /** 대기 중인 작업을 모두 돌린다. `advanceUntilIdle`만으로는 backgroundScope 작업이 돌지 않는다. */
    protected fun TestScope.settle() {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun startsWaitingWithoutTask() = runTest {
        val controller = create()
        settle()
        assertEquals(SessionState.WAITING, controller.state.value.state)
        assertNull(controller.state.value.taskId)
    }

    @Test
    fun startTaskEntersSessionForThatTask() = runTest {
        val controller = create()
        controller.startTask("t1")
        settle()
        assertEquals("t1", controller.state.value.taskId)
        assertNotEquals(SessionState.WAITING, controller.state.value.state)
    }

    @Test
    fun stopReturnsToWaiting() = runTest {
        val controller = create()
        controller.startTask("t1")
        settle()
        controller.stop()
        settle()
        assertEquals(SessionState.WAITING, controller.state.value.state)
        assertNull(controller.state.value.taskId)
    }

    @Test
    fun stopWithoutStartIsHarmless() = runTest {
        val controller = create()
        controller.stop()
        settle()
        assertEquals(SessionState.WAITING, controller.state.value.state)
    }

    @Test
    fun interventionLevelStaysInRange() = runTest {
        val controller = create()
        controller.startTask("t1")
        settle()
        assertTrue("shared/states.md: interventionLevel은 0~3", controller.state.value.interventionLevel in 0..3)
    }
}
