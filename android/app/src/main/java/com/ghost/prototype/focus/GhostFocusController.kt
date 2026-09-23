package com.ghost.prototype.focus

import com.ghost.prototype.contract.FocusController
import com.ghost.prototype.contract.FocusError
import com.ghost.prototype.contract.FocusSnapshot
import com.ghost.prototype.contract.SessionState
import com.ghost.prototype.detection.AppObservation
import com.ghost.prototype.floating.FloatingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * `FocusController`의 실제 구현. `FakeFocusController`를 대체한다(#61).
 *
 * 하는 일은 셋뿐이다.
 * 1. 감지 관측을 [RuleJudge]로 판정한다
 * 2. 판정을 [FocusStateMachine]에 넣어 딴짓 시간을 누적하고 개입 레벨을 올린다
 * 3. 결과를 [FocusSnapshot]으로 UI에 알린다
 *
 * **판단 로직은 전부 [FocusStateMachine]에 있다.** 여기는 배선만 한다 —
 * 그래야 상태머신을 Android 없이 단위 테스트할 수 있다.
 *
 * 서버 동기화는 아직 없다(#14). 세션은 프로세스 메모리에만 있다.
 */
class GhostFocusController(
    private val startFloating: () -> Unit,
    private val stopFloating: () -> Unit,
    floating: StateFlow<FloatingState>,
    /** 최근 관측된 외부 앱. `DetectionStateStore.state`에서 온다. */
    observations: StateFlow<AppObservation?>,
    private val judge: RuleJudge,
    private val overlayGranted: () -> Boolean,
    private val scope: CoroutineScope,
    private val policy: InterventionPolicy = InterventionPolicy(),
    /** 테스트에서 시간을 제어하기 위해 주입한다. */
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val delayMillis: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) : FocusController {

    private val machine = FocusStateMachine(policy)
    private val taskId = MutableStateFlow<String?>(null)
    private val machineState = MutableStateFlow(machine.state)

    /** 현재 판정과 그 판정이 시작된 시각. 유지 시간(`minHoldSeconds`) 계산에 쓴다. */
    private var currentJudgement: Judgement? = null
    private var judgementSince: Long = 0
    private var lastTickAt: Long = 0
    private var ticker: Job? = null

    init {
        observations
            .onEach { onObserved(it) }
            .launchIn(scope)
    }

    /**
     * 세션이 도는 동안 1초마다 누적을 갱신한다. [startTask]에서 시작하고 [stop]에서 끝난다.
     *
     * 세션이 끝나면 루프도 끝난다 — 대기 중에 타이머를 돌릴 이유가 없고,
     * 끝나지 않는 루프는 테스트에서 `advanceUntilIdle`을 멈추지 못하게 만든다.
     */
    private fun startTicking() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && machine.state.session != SessionState.WAITING) {
                delayMillis(TICK_MILLIS)
                tick()
            }
        }
    }

    override val state: StateFlow<FocusSnapshot> =
        combine(taskId, machineState, floating) { task, machine, float ->
            FocusSnapshot(
                taskId = task,
                state = machine.session,
                interventionLevel = machine.level,
                distractSeconds = machine.distractSeconds,
                floatingVisible = float.visible,
                error = float.error?.let {
                    if (overlayGranted()) FocusError.OVERLAY_ATTACH_FAILED
                    else FocusError.OVERLAY_PERMISSION_MISSING
                },
            )
        }.stateIn(scope, SharingStarted.Eagerly, FocusSnapshot())

    override fun startTask(taskId: String) {
        this.taskId.value = taskId
        machine.start()
        publish()
        currentJudgement = null
        judgementSince = nowMillis()
        lastTickAt = nowMillis()
        startTicking()
        startFloating()
    }

    override fun stop() {
        taskId.value = null
        machine.stop()
        publish()
        ticker?.cancel()
        ticker = null
        currentJudgement = null
        stopFloating()
    }

    /** 스누즈(#64). 잔여 시간 동안 개입 레벨이 오르지 않는다. */
    fun snooze(seconds: Int) {
        machine.snooze(seconds)
        publish()
    }

    /** 세션 누적 딴짓 시간(#62). `state.value.distractSeconds`와 같다. */
    val distractSeconds: Int get() = machineState.value.distractSeconds

    private fun onObserved(observation: AppObservation?) {
        val judgement = observation?.packageName?.let(judge::judge)
        if (judgement != currentJudgement) {
            // 앱이 바뀌었다. 유지 시간을 다시 센다.
            currentJudgement = judgement
            judgementSince = nowMillis()
        }
    }

    private fun tick() {
        val now = nowMillis()
        val elapsed = ((now - lastTickAt) / 1000).toInt()
        if (elapsed <= 0) return

        val held = ((now - judgementSince) / 1000).toInt()
        val observation = currentJudgement?.let { Observation(it, held) }

        // 유지 시간을 아직 못 채웠으면 시간을 흘려보내지 않는다. `lastTickAt` 을 그대로 두어
        // 판정이 확정되는 순간 그동안의 시간이 한꺼번에 누적되게 한다.
        //
        // 이렇게 하지 않으면 `minHoldSeconds` 만큼이 사라져서, 10분을 봤는데 누적은 9분 40초가 된다.
        // "유튜브 본 지 10분 됐어"는 실제 경과 시간이어야 한다.
        if (observation != null && held < policy.minHoldSeconds) return

        lastTickAt = now
        machine.tick(elapsed, observation)
        publish()
    }

    private fun publish() {
        machineState.value = machine.state
    }

    companion object {
        private const val TICK_MILLIS = 1_000L
    }
}
