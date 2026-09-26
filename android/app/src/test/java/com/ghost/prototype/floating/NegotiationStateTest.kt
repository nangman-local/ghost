package com.ghost.prototype.floating

import org.junit.Assert.assertEquals
import org.junit.Test

/** `NegotiationMachine`의 상태 전이. `shared/states.md`의 `interventionLevel = 2`("복귀 제안")를 채운다. */
class NegotiationStateTest {

    private val machine = NegotiationMachine(countdownSeconds = 120)

    @Test
    fun `처음에는 아무것도 보여주지 않는다`() {
        assertEquals(NegotiationState.Hidden, machine.state)
    }

    @Test
    fun `레벨 2 도달 시 할 일 제목으로 제안한다`() {
        machine.offer("보고서 쓰기")
        assertEquals(NegotiationState.Offer("보고서 쓰기"), machine.state)
    }

    @Test
    fun `이미 제안 중이면 다시 제안해도 무시한다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.offer("다른 할 일") // 매 틱마다 offer()가 다시 불려도 카운트다운을 덮어쓰면 안 된다
        assertEquals(NegotiationState.Counting("보고서 쓰기", 120), machine.state)
    }

    @Test
    fun `수락하면 카운트다운이 시작된다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        assertEquals(NegotiationState.Counting("보고서 쓰기", 120), machine.state)
    }

    @Test
    fun `제안 상태가 아니면 수락은 무시한다`() {
        machine.accept() // Hidden 상태에서 수락
        assertEquals(NegotiationState.Hidden, machine.state)
    }

    @Test
    fun `제안 직후에는 거절 선택지가 안 보인다`() {
        // "해볼래"만 먼저 보여준다. 유령을 한 번 더 탭해야(revealReject) "아니, 됐어"가 나온다.
        machine.offer("보고서 쓰기")
        assertEquals(NegotiationState.Offer("보고서 쓰기", rejectVisible = false), machine.state)
    }

    @Test
    fun `아직 안 열어본 거절은 무시한다`() {
        machine.offer("보고서 쓰기")
        machine.reject()
        assertEquals(
            "화면에 없던 버튼이 눌릴 수는 없다",
            NegotiationState.Offer("보고서 쓰기", rejectVisible = false),
            machine.state,
        )
    }

    @Test
    fun `한 번 더 탭하면 거절 선택지가 보인다`() {
        machine.offer("보고서 쓰기")
        machine.revealReject()
        assertEquals(NegotiationState.Offer("보고서 쓰기", rejectVisible = true), machine.state)
    }

    @Test
    fun `열어본 뒤에는 거절하면 닫힌다`() {
        machine.offer("보고서 쓰기")
        machine.revealReject()
        machine.reject()
        assertEquals(NegotiationState.Hidden, machine.state)
    }

    @Test
    fun `카운트다운 중 거절은 무시한다 - 이미 수락한 것을 취소할 수 없다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.reject()
        assertEquals(NegotiationState.Counting("보고서 쓰기", 120), machine.state)
    }

    @Test
    fun `틱이 흐르면 남은 시간이 줄어든다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.tick(30)
        assertEquals(NegotiationState.Counting("보고서 쓰기", 90), machine.state)
    }

    @Test
    fun `카운트다운이 0에 닿으면 축하 단계로 넘어간다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.tick(120)
        assertEquals(NegotiationState.Celebrate("보고서 쓰기"), machine.state)
    }

    @Test
    fun `카운트다운을 넘겨도(지연) 축하 단계로 넘어간다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.tick(150)
        assertEquals(NegotiationState.Celebrate("보고서 쓰기"), machine.state)
    }

    @Test
    fun `제안이나 축하 상태가 아니면 틱은 아무 효과가 없다`() {
        machine.offer("보고서 쓰기")
        machine.tick(30) // 아직 Offer 상태 — 수락 전에는 시간이 흐르지 않는다
        assertEquals(NegotiationState.Offer("보고서 쓰기"), machine.state)
    }

    @Test
    fun `더 할래를 고르면 같은 할 일로 카운트다운을 다시 돈다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.tick(120)
        machine.continueMore()
        assertEquals(NegotiationState.Counting("보고서 쓰기", 120), machine.state)
    }

    @Test
    fun `쉴래를 고르면 닫힌다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.tick(120)
        machine.rest()
        assertEquals(NegotiationState.Hidden, machine.state)
    }

    @Test
    fun `레벨이 복귀하면(clear) 어느 단계에 있든 닫힌다`() {
        machine.offer("보고서 쓰기")
        machine.accept()
        machine.clear()
        assertEquals(NegotiationState.Hidden, machine.state)
    }
}
