package com.ghost.prototype.floating

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `NegotiationMachine` 자체의 규칙은 `NegotiationStateTest`에서 본다. 여기는 배선을 본다 —
 * 개입 레벨·할 일 제목을 구독해 상태머신에 넣고, 카운트다운 중에만 1초 틱을 돈다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NegotiationControllerTest {

    private class Fixture(
        val controller: NegotiationController,
        val level: MutableStateFlow<Int>,
        val title: MutableStateFlow<String?>,
        val advance: (Int) -> Unit,
    )

    private fun TestScope.build(): Fixture {
        var now = 1_000_000L
        val level = MutableStateFlow(0)
        val title = MutableStateFlow<String?>(null)

        val controller = NegotiationController(
            interventionLevel = level,
            currentTaskTitle = title,
            fallbackTitle = "지금 하는 일",
            scope = backgroundScope,
            nowMillis = { now },
            delayMillis = { millis -> delay(millis); now += millis },
        )
        return Fixture(
            controller = controller,
            level = level,
            title = title,
            advance = { seconds -> testScheduler.advanceTimeBy(seconds * 1000L) },
        )
    }

    private fun TestScope.settle() {
        testScheduler.advanceUntilIdle()
        testScheduler.runCurrent()
    }

    @Test
    fun `레벨 2가 돼도 자동으로 제안하지 않는다`() = runTest {
        // 유령을 탭해야(reveal) 연다 — 화면에 예고 없이 튀어나오지 않는다.
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()

        assertEquals(NegotiationState.Hidden, f.controller.state.value)
    }

    @Test
    fun `레벨 2에서 탭하면 할 일 제목으로 제안한다`() = runTest {
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()

        val revealed = f.controller.reveal()
        settle()

        assertTrue("제안할 게 있으면 reveal()이 true여야 한다", revealed)
        assertEquals(NegotiationState.Offer("보고서 쓰기"), f.controller.state.value)
    }

    @Test
    fun `레벨 1 이하에서 탭해도 아무 일도 없다`() = runTest {
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 1
        settle()

        val revealed = f.controller.reveal()
        settle()

        assertFalse("제안할 게 없으면 reveal()은 false여야 한다", revealed)
        assertEquals(NegotiationState.Hidden, f.controller.state.value)
    }

    @Test
    fun `할 일 제목이 없으면 대체 문구를 쓴다`() = runTest {
        // 개발자 도구의 진단 세션처럼 실제 할 일이 없는 경우(diagnostic).
        val f = build()
        f.level.value = 2
        settle()
        f.controller.reveal()
        settle()

        assertEquals(NegotiationState.Offer("지금 하는 일"), f.controller.state.value)
    }

    @Test
    fun `수락하면 카운트다운이 실제로 흐른다`() = runTest {
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()
        f.controller.reveal()
        settle()

        f.controller.accept()
        settle()
        f.advance(30)
        settle()

        val state = f.controller.state.value
        assert(state is NegotiationState.Counting && state.remainingSeconds <= 90) {
            "30초가 흘렀으면 남은 시간이 줄어야 한다 (실제: $state)"
        }
    }

    @Test
    fun `레벨이 복귀하면 카운트다운 중이어도 정리된다`() = runTest {
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()
        f.controller.reveal()
        settle()
        f.controller.accept()
        settle()

        f.level.value = 0 // 사용자가 실제로 복귀함
        settle()

        assertEquals(NegotiationState.Hidden, f.controller.state.value)
    }

    @Test
    fun `레벨이 복귀하면 카운트다운 틱이 멈춘다`() = runTest {
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()
        f.controller.reveal()
        settle()
        f.controller.accept()
        settle()

        f.level.value = 0
        settle()
        f.level.value = 2 // 다시 딴짓
        settle()

        // 레벨이 다시 2가 돼도 자동으로 뜨지 않는다 — 또 탭해야 한다.
        assertEquals(NegotiationState.Hidden, f.controller.state.value)
        f.controller.reveal()
        settle()
        assertEquals(
            "새 제안이 떠야지, 멈춰 있던 카운트다운이 이어지면 안 된다",
            NegotiationState.Offer("보고서 쓰기"),
            f.controller.state.value,
        )
    }

    @Test
    fun `탭 한 번으로는 거절 선택지가 안 뜬다`() = runTest {
        // "해볼래"만 먼저 보여준다.
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()

        f.controller.reveal()
        settle()

        assertEquals(NegotiationState.Offer("보고서 쓰기", rejectVisible = false), f.controller.state.value)
    }

    @Test
    fun `탭을 두 번 하면 거절 선택지가 뜬다`() = runTest {
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()

        f.controller.reveal() // 1번째: 제안을 연다
        settle()
        val secondTap = f.controller.reveal() // 2번째: 거절 선택지를 연다
        settle()

        assertTrue(secondTap)
        assertEquals(NegotiationState.Offer("보고서 쓰기", rejectVisible = true), f.controller.state.value)
    }

    @Test
    fun `거절해도 레벨 자체는 이 컨트롤러가 건드리지 않는다`() = runTest {
        // 협상은 딴짓 판단과 별개다 — 거절은 표시만 닫을 뿐, 딴짓 누적/레벨은 FocusStateMachine 몫이다.
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()
        f.controller.reveal()
        f.controller.reveal() // 거절 선택지를 열어야 실제로 거절할 수 있다
        settle()

        f.controller.reject()
        settle()

        assertEquals(NegotiationState.Hidden, f.controller.state.value)
        assertEquals(2, f.level.value) // 컨트롤러가 레벨을 되돌리지 않았다
    }

    @Test
    fun `거절한 뒤 다시 탭하면 또 열린다`() = runTest {
        // reveal()은 레벨 2인 동안 "안 열어봤을 때"뿐 아니라 거절 후 재탭에도 응한다 —
        // 사용자가 스스로 다시 확인하는 것까지 막을 이유는 없다.
        val f = build()
        f.title.value = "보고서 쓰기"
        f.level.value = 2
        settle()
        f.controller.reveal()
        f.controller.reveal()
        f.controller.reject()
        settle()

        val revealedAgain = f.controller.reveal()
        settle()

        assertTrue(revealedAgain)
        assertEquals(NegotiationState.Offer("보고서 쓰기", rejectVisible = false), f.controller.state.value)
    }
}
