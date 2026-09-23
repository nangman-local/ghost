package com.ghost.prototype.focus

import com.ghost.prototype.contract.SessionState

/** 로컬 규칙 판단 결과. `shared/states.md`의 판정 라벨 `label`. */
enum class Judgement { FOCUS, DISTRACT, AMBIGUOUS }

/**
 * 상태머신이 보는 세계. Android 타입이 들어오지 않는다(순수 Kotlin).
 *
 * @param judgement 현재 관측의 판정
 * @param heldSeconds 이 판정이 유지된 시간
 */
data class Observation(val judgement: Judgement, val heldSeconds: Int)

/**
 * 개입 상태머신의 한 시점 상태.
 *
 * @param distractSeconds 세션 누적 딴짓 시간(#62). 연속이 아니라 합계다.
 * @param focusStreakSeconds 연속 `FOCUS` 유지 시간. 복귀 판정에 쓴다.
 * @param snoozeRemainingSeconds 스누즈 잔여 시간(#64). 0보다 크면 개입하지 않는다.
 */
data class FocusMachineState(
    val session: SessionState = SessionState.WAITING,
    val level: Int = 0,
    val distractSeconds: Int = 0,
    val focusStreakSeconds: Int = 0,
    val snoozeRemainingSeconds: Int = 0,
)

/**
 * 딴짓 누적 → 개입 레벨 승급 로직. **이 서비스의 핵심 가설을 구현한 곳이다.**
 *
 * Android 의존이 없어 단위 테스트로 전부 검증한다(`FocusStateMachineTest`).
 * 규칙은 `shared/states.md`의 "개입 전이 규칙"을 그대로 따른다.
 *
 * 시간은 호출자가 [tick]으로 넣는다. 내부에 타이머를 두지 않는다 — 테스트에서 시간을 제어하기 위해서다.
 */
class FocusStateMachine(private val policy: InterventionPolicy = InterventionPolicy()) {

    var state: FocusMachineState = FocusMachineState()
        private set

    /** 할 일을 시작한다. `WAITING → FOCUS`. 누적은 0에서 출발한다. */
    fun start() {
        state = FocusMachineState(session = SessionState.FOCUS)
    }

    /** 세션을 끝낸다. `* → WAITING`. */
    fun stop() {
        state = FocusMachineState()
    }

    /** 스누즈를 건다(#64). 잔여 시간 동안 레벨이 오르지 않는다. 레벨 자체는 유지한다. */
    fun snooze(seconds: Int) {
        if (state.session == SessionState.WAITING) return
        state = state.copy(snoozeRemainingSeconds = maxOf(0, seconds))
    }

    /**
     * [elapsedSeconds]만큼 시간이 흘렀다. [observation]은 그동안의 관측이다.
     *
     * 관측이 없으면(null) 시간만 흐르고 판정은 하지 않는다 — 권한이 없거나 감지가 실패한 경우다.
     * **감지 실패는 딴짓이 아니다.** 누적하지 않는다.
     */
    fun tick(elapsedSeconds: Int, observation: Observation?) {
        if (state.session == SessionState.WAITING || elapsedSeconds <= 0) return

        val next = state.copy(
            snoozeRemainingSeconds = maxOf(0, state.snoozeRemainingSeconds - elapsedSeconds),
        )

        // 판정할 수 없으면 시간만 흐른다. AMBIGUOUS도 같다 — 판단하지 않고 개입하지도 않는다.
        val judgement = observation
            ?.takeIf { it.heldSeconds >= policy.minHoldSeconds }
            ?.judgement
        if (judgement == null || judgement == Judgement.AMBIGUOUS) {
            state = next
            return
        }

        state = when (judgement) {
            Judgement.DISTRACT -> next.onDistract(elapsedSeconds)
            Judgement.FOCUS -> next.onFocus(elapsedSeconds)
            Judgement.AMBIGUOUS -> next // 위에서 걸러진다
        }
    }

    private fun FocusMachineState.onDistract(elapsed: Int): FocusMachineState {
        val accumulated = distractSeconds + elapsed
        val session =
            if (accumulated >= policy.distractEntrySeconds) SessionState.DISTRACT else SessionState.FOCUS

        // 스누즈 중에는 레벨이 오르지 않는다. 누적은 계속한다(스누즈로 시간을 되돌릴 수는 없다).
        val level = if (snoozeRemainingSeconds > 0) {
            this.level
        } else {
            // 한 번에 한 단계씩만 올라간다. 레벨은 내려가지 않는다.
            (policy.levelFor(accumulated)).coerceAtMost(this.level + 1).coerceAtLeast(this.level)
        }

        return copy(
            session = session,
            level = level,
            distractSeconds = accumulated,
            focusStreakSeconds = 0,
        )
    }

    private fun FocusMachineState.onFocus(elapsed: Int): FocusMachineState {
        val streak = focusStreakSeconds + elapsed
        // 유예 시간 이상 집중했으면 진짜 복귀로 본다. 누적과 레벨을 초기화한다.
        return if (streak >= policy.recoveryGraceSeconds) {
            copy(
                session = SessionState.FOCUS,
                level = 0,
                distractSeconds = 0,
                focusStreakSeconds = streak,
            )
        } else {
            // 아직 복귀로 인정하지 않는다. 누적과 레벨을 유지한다.
            copy(focusStreakSeconds = streak)
        }
    }
}
