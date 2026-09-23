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

    /** 누적 딴짓 시간은 음수가 될 수 없고, 세션이 없으면 0이다(#71). */
    @Test
    fun distractSecondsIsNeverNegative() = runTest {
        val controller = create()
        settle()
        assertEquals("세션 전에는 0", 0, controller.state.value.distractSeconds)

        controller.startTask("t1")
        settle()
        assertTrue("음수일 수 없다", controller.state.value.distractSeconds >= 0)

        controller.stop()
        settle()
        assertEquals("세션이 끝나면 0", 0, controller.state.value.distractSeconds)
    }

    /**
     * 세션은 기기 안에 하나뿐이다(#52). 어느 화면에서 시작하든 같은 세션을 본다.
     * `shared/api-schema.md`의 `devices[].state`가 기기당 하나이므로, 기기 안에서 먼저 하나여야 한다.
     */
    @Test
    fun sessionIsSingleAcrossCallers() = runTest {
        val controller = create()

        // 한쪽(예: 개발자 도구)에서 시작하면
        controller.startTask("t1")
        settle()

        // 다른 쪽(예: 홈)이 보는 상태도 같아야 한다. 같은 인스턴스를 보므로 상태가 하나다.
        assertEquals("t1", controller.state.value.taskId)
        assertNotEquals(SessionState.WAITING, controller.state.value.state)

        // 다른 할 일로 다시 시작하면 세션이 갈라지지 않고 교체된다.
        controller.startTask("t2")
        settle()
        assertEquals("두 세션이 동시에 살아 있으면 안 된다", "t2", controller.state.value.taskId)

        // 한쪽에서 끝내면 모두에게 끝난 것이다.
        controller.stop()
        settle()
        assertEquals(SessionState.WAITING, controller.state.value.state)
        assertNull(controller.state.value.taskId)
    }
}
