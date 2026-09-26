package com.ghost.prototype.floating

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [NegotiationMachine]의 배선(#63). `GhostFocusController`와 같은 자리 — 판단은 전부
 * 순수 로직에 있고, 여기는 개입 레벨·할 일 제목을 구독해 상태머신에 넣고 1초 카운트다운을 돈다.
 *
 * **레벨 2가 된다고 제안이 자동으로 뜨지 않는다.** 유령을 탭해야([reveal]) 그제서야 연다 —
 * 예측할 수 없는 순간에 화면 위로 튀어나오지 않게 하려는 의도다.
 */
class NegotiationController(
    /** `FocusController.state`에서 뽑는다. */
    interventionLevel: StateFlow<Int>,
    /** `TaskRepository.currentTask`에서 뽑는다. 세션의 할 일이 실제 목록에 없으면(개발자 도구 진단 세션) null. */
    currentTaskTitle: StateFlow<String?>,
    /** 할 일 제목을 못 구했을 때 쓸 문구. Android `Context`가 필요해 호출자가 넘긴다. */
    private val fallbackTitle: String,
    private val scope: CoroutineScope,
    countdownSeconds: Int = NegotiationMachine.DEFAULT_COUNTDOWN_SECONDS,
    /** 테스트에서 시간을 제어하기 위해 주입한다. */
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val delayMillis: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {
    private val machine = NegotiationMachine(countdownSeconds)
    private val mutableState = MutableStateFlow(machine.state)
    val state: StateFlow<NegotiationState> = mutableState.asStateFlow()

    private var ticker: Job? = null
    private var lastTickAt = 0L

    /**
     * 레벨 2에 도달해 제안할 게 생겼지만 아직 [reveal]로 열어보지 않은 할 일 제목.
     * **레벨 2가 된다고 자동으로 뜨지 않는다** — 유령을 탭해야 연다. 화면 아무 데서나
     * 예고 없이 튀어나오는 것을 막고, 캐릭터 탭 인터랙션(#64)과도 자연스럽게 이어진다.
     */
    private var eligibleTitle: String? = null

    init {
        combine(interventionLevel, currentTaskTitle) { level, title -> level to title }
            .distinctUntilChanged()
            .onEach { (level, title) -> onLevelChanged(level, title) }
            .launchIn(scope)
    }

    /**
     * 유령을 탭했다. 두 가지 경우에 반응한다.
     * 1. 제안할 게 있는데 아직 안 열어봤으면(레벨 2, `Hidden`) — 연다("해볼래"만 보이는 상태로).
     * 2. 이미 제안이 열려 있는데 거절 선택지를 아직 안 보여줬으면 — 그제서야 "아니, 됐어"를 보여준다.
     *    처음부터 두 선택지를 다 보여주지 않고 한 번 더 탭해야 나오게 해서, 수락 쪽으로 살짝
     *    기울인다. 거부 자체를 막는 건 아니다(#64: "출구가 없으면 사용자는 앱을 지운다").
     *
     * @return 뭔가 반응했으면 true. 캐릭터 탭 처리 쪽에서 false면 평소 동작("안녕?")으로 넘어간다.
     */
    fun reveal(): Boolean {
        val current = machine.state
        if (current is NegotiationState.Offer && !current.rejectVisible) {
            machine.revealReject()
            publish()
            return true
        }
        val title = eligibleTitle ?: return false
        machine.offer(title)
        publish()
        return true
    }

    fun accept() {
        machine.accept()
        publish()
        startTickingIfCounting()
    }

    fun reject() {
        machine.reject()
        publish()
    }

    fun continueMore() {
        machine.continueMore()
        publish()
        startTickingIfCounting()
    }

    fun rest() {
        machine.rest()
        publish()
    }

    private fun onLevelChanged(level: Int, title: String?) {
        if (level >= 2) {
            eligibleTitle = title ?: fallbackTitle
        } else {
            eligibleTitle = null
            machine.clear()
        }
        publish()
        if (machine.state !is NegotiationState.Counting) stopTicking()
    }

    /**
     * 카운트다운 중에만 1초마다 [NegotiationMachine.tick]을 부른다.
     * 카운트다운이 끝나거나(축하) 정리되면(레벨 복귀) 루프도 끝난다.
     */
    private fun startTickingIfCounting() {
        if (machine.state !is NegotiationState.Counting || ticker?.isActive == true) return
        lastTickAt = nowMillis()
        ticker = scope.launch {
            while (isActive && machine.state is NegotiationState.Counting) {
                delayMillis(TICK_MILLIS)
                val now = nowMillis()
                val elapsed = ((now - lastTickAt) / 1000).toInt()
                if (elapsed <= 0) continue
                lastTickAt = now
                machine.tick(elapsed)
                publish()
            }
        }
    }

    private fun stopTicking() {
        ticker?.cancel()
        ticker = null
    }

    private fun publish() {
        mutableState.value = machine.state
    }

    companion object {
        private const val TICK_MILLIS = 1_000L
    }
}
