package com.ghost.prototype.floating

/**
 * 2분 협상(#63)의 한 시점 상태. `shared/states.md`의 `interventionLevel = 2`("복귀 제안")를
 * 실제로 채우는 내용이다. Android 의존이 없어 단위 테스트로 검증한다.
 */
sealed interface NegotiationState {
    /** 보여줄 게 없다. 레벨 0~1이거나, 거절/휴식으로 닫힌 상태. */
    data object Hidden : NegotiationState

    /**
     * "[할 일] 딱 2분만 해볼까?" 제안. 수락을 기다린다.
     *
     * @param rejectVisible 거절 선택지를 보여줄지. **처음 열리면 false다** — "해볼래"만 먼저
     *   보여주고, 유령을 한 번 더 탭해야 "아니, 됐어"가 나온다. 거부 자체를 막는 건 아니다
     *   (#64: "출구가 없으면 앱을 지운다"), 한 번 더 확인하는 절차를 둘 뿐이다.
     */
    data class Offer(val taskTitle: String, val rejectVisible: Boolean = false) : NegotiationState

    /** 수락 후 카운트다운 중. */
    data class Counting(val taskTitle: String, val remainingSeconds: Int) : NegotiationState

    /** 카운트다운이 끝났다. "더 할래 / 쉴래"를 묻는다. */
    data class Celebrate(val taskTitle: String) : NegotiationState
}

/**
 * 2분 협상의 상태 전이. [FocusStateMachine](../focus/FocusStateMachine.kt)과 같은 자리(순수 Kotlin,
 * Android 타입 없음)에 둔다 — `android/AGENTS.md`: Android 독립 로직은 분리해 단위 테스트한다.
 *
 * **딴짓 판단과는 별개다.** 이 상태머신은 개입 레벨이 2인 동안 무엇을 보여줄지만 결정한다.
 * 딴짓 누적·레벨 자체는 여전히 `FocusStateMachine`이 정한다 — 협상을 거절해도 레벨은 그대로다
 * (스누즈로 레벨 승급을 늦추는 것은 #64의 몫이다).
 *
 * 시간은 호출자가 [tick]으로 넣는다. 내부에 타이머를 두지 않는다(테스트에서 시간 제어).
 */
class NegotiationMachine(private val countdownSeconds: Int = DEFAULT_COUNTDOWN_SECONDS) {
    var state: NegotiationState = NegotiationState.Hidden
        private set

    /**
     * 개입 레벨이 2 이상이 됐다. 이미 뭔가 보여주고 있으면(제안/카운트다운/축하) 다시 제안하지 않는다 —
     * 한 번 거절했다고 매초 다시 들이밀지 않는다.
     */
    fun offer(taskTitle: String) {
        if (state == NegotiationState.Hidden) state = NegotiationState.Offer(taskTitle)
    }

    /** 개입 레벨이 2 밑으로 내려갔다(복귀). 무엇을 보여주고 있었든 정리한다. */
    fun clear() {
        state = NegotiationState.Hidden
    }

    fun accept() {
        val offer = state as? NegotiationState.Offer ?: return
        state = NegotiationState.Counting(offer.taskTitle, countdownSeconds)
    }

    /** 유령을 한 번 더 탭했다. 제안 중이고 아직 거절 선택지를 안 보여줬으면 그제서야 보여준다. */
    fun revealReject() {
        val offer = state as? NegotiationState.Offer ?: return
        if (!offer.rejectVisible) state = offer.copy(rejectVisible = true)
    }

    /**
     * 거절하면 닫는다. 레벨은 `FocusStateMachine`이 계속 관리하므로 여기서 건드리지 않는다.
     * **[revealReject]로 열어보지 않았으면 무시한다** — 화면에 없던 버튼이 눌릴 수는 없다.
     */
    fun reject() {
        val offer = state as? NegotiationState.Offer ?: return
        if (offer.rejectVisible) state = NegotiationState.Hidden
    }

    /** 카운트다운 중 [elapsedSeconds]가 흐른다. 0 이하가 되면 축하 단계로 넘어간다. */
    fun tick(elapsedSeconds: Int) {
        val counting = state as? NegotiationState.Counting ?: return
        val remaining = counting.remainingSeconds - elapsedSeconds
        state = if (remaining <= 0) NegotiationState.Celebrate(counting.taskTitle)
        else counting.copy(remainingSeconds = remaining)
    }

    /** "더 할래" — 같은 할 일로 카운트다운을 한 번 더 돈다. */
    fun continueMore() {
        val celebrate = state as? NegotiationState.Celebrate ?: return
        state = NegotiationState.Counting(celebrate.taskTitle, countdownSeconds)
    }

    /** "쉴래" — 닫는다. */
    fun rest() {
        if (state is NegotiationState.Celebrate) state = NegotiationState.Hidden
    }

    companion object {
        const val DEFAULT_COUNTDOWN_SECONDS = 120
    }
}
