package com.ghost.prototype.focus

import com.ghost.prototype.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `shared/states.md`의 "개입 전이 규칙"을 그대로 검증한다.
 * 임계값이 바뀌면(#10 도그푸딩 후) 이 테스트의 기대값도 같이 바뀐다.
 */
class FocusStateMachineTest {

    private val policy = InterventionPolicy()

    /** 판정이 확정되도록 충분히 유지된 관측. */
    private fun distract(held: Int = 60) = Observation(Judgement.DISTRACT, held)
    private fun focus(held: Int = 60) = Observation(Judgement.FOCUS, held)

    /** [seconds]만큼 [observation]이 계속됐다고 알린다. 1초 단위로 쪼개 넣는다. */
    private fun FocusStateMachine.run(seconds: Int, observation: Observation?) {
        repeat(seconds) { tick(1, observation) }
    }

    // --- 기본 전이 ---

    @Test
    fun `시작 전에는 대기 상태다`() {
        val machine = FocusStateMachine(policy)
        assertEquals(SessionState.WAITING, machine.state.session)
        assertEquals(0, machine.state.level)
    }

    @Test
    fun `대기 중에는 딴짓을 누적하지 않는다`() {
        val machine = FocusStateMachine(policy)
        machine.run(600, distract())
        assertEquals(SessionState.WAITING, machine.state.session)
        assertEquals(0, machine.state.distractSeconds)
    }

    @Test
    fun `시작하면 집중 상태다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        assertEquals(SessionState.FOCUS, machine.state.session)
    }

    @Test
    fun `딴짓이 진입 임계값을 넘으면 DISTRACT로 간다`() {
        val machine = FocusStateMachine(policy).apply { start() }

        machine.run(policy.distractEntrySeconds - 1, distract())
        assertEquals("아직 진입 전", SessionState.FOCUS, machine.state.session)

        machine.run(1, distract())
        assertEquals(SessionState.DISTRACT, machine.state.session)
    }

    @Test
    fun `종료하면 누적과 레벨이 사라진다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(700, distract())
        machine.stop()

        assertEquals(SessionState.WAITING, machine.state.session)
        assertEquals(0, machine.state.level)
        assertEquals(0, machine.state.distractSeconds)
    }

    // --- 레벨 승급 ---

    @Test
    fun `10분이면 1단계로 올라간다`() {
        val machine = FocusStateMachine(policy).apply { start() }

        machine.run(policy.levelSeconds[0] - 1, distract())
        assertEquals("아직 승급 전", 0, machine.state.level)

        machine.run(1, distract())
        assertEquals(1, machine.state.level)
    }

    @Test
    fun `15분이면 2단계로 올라간다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(policy.levelSeconds[1], distract())
        assertEquals(2, machine.state.level)
    }

    @Test
    fun `MVP는 2단계를 넘지 않는다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(60 * 60, distract()) // 한 시간 내리 딴짓

        assertEquals("3단계는 #22. maxLevel로 막는다", 2, machine.state.level)
    }

    @Test
    fun `레벨은 한 번에 한 단계씩만 올라간다`() {
        val machine = FocusStateMachine(policy).apply { start() }

        // 20분치를 한 번에 넣어도 0 → 1 을 밟는다
        machine.tick(policy.levelSeconds[2], distract(held = policy.levelSeconds[2]))
        assertEquals(1, machine.state.level)

        machine.tick(1, distract())
        assertEquals(2, machine.state.level)
    }

    @Test
    fun `개입 레벨은 항상 0에서 3 사이다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        repeat(200) {
            machine.tick(30, distract())
            assert(machine.state.level in 0..3) { "shared/states.md: 0~3 범위" }
        }
    }

    // --- 복귀 ---

    @Test
    fun `유예 시간 이상 집중하면 누적과 레벨이 초기화된다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(policy.levelSeconds[0], distract())
        assertEquals(1, machine.state.level)

        machine.run(policy.recoveryGraceSeconds, focus())

        assertEquals(0, machine.state.level)
        assertEquals(0, machine.state.distractSeconds)
        assertEquals(SessionState.FOCUS, machine.state.session)
    }

    @Test
    fun `짧게 복귀한 것으로는 초기화되지 않는다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(policy.levelSeconds[0], distract())

        machine.run(policy.recoveryGraceSeconds - 1, focus())

        assertEquals("아직 진짜 복귀가 아니다", 1, machine.state.level)
        assertEquals(policy.levelSeconds[0], machine.state.distractSeconds)
    }

    @Test
    fun `잠깐씩 복귀해도 누적은 이어진다`() {
        val machine = FocusStateMachine(policy).apply { start() }

        // 5분 딴짓 → 잠깐 복귀 → 5분 딴짓. 누적은 10분이 되어 1단계에 도달한다.
        machine.run(5 * 60, distract())
        machine.run(policy.recoveryGraceSeconds - 1, focus())
        machine.run(5 * 60, distract())

        assertEquals("누적은 합계다", 1, machine.state.level)
    }

    // --- 판정하지 않는 경우 ---

    @Test
    fun `AMBIGUOUS는 누적하지 않는다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(20 * 60, Observation(Judgement.AMBIGUOUS, 60))

        assertEquals("모호하면 개입하지 않는다", 0, machine.state.level)
        assertEquals(0, machine.state.distractSeconds)
    }

    @Test
    fun `관측이 없으면 누적하지 않는다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(20 * 60, null)

        assertEquals("감지 실패는 딴짓이 아니다", 0, machine.state.level)
        assertEquals(0, machine.state.distractSeconds)
    }

    @Test
    fun `유지 시간이 짧으면 판정하지 않는다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(20 * 60, distract(held = policy.minHoldSeconds - 1))

        assertEquals("카톡 확인 같은 짧은 행동", 0, machine.state.distractSeconds)
    }

    // --- 스누즈 (#64) ---

    @Test
    fun `스누즈 중에는 레벨이 오르지 않는다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(policy.levelSeconds[0], distract())
        assertEquals(1, machine.state.level)

        // 2단계 임계값을 넘기고도 남을 만큼 딴짓하지만, 스누즈가 끝나기 전까지는 승급하지 않는다.
        val snooze = 20 * 60
        machine.snooze(snooze)
        machine.run(snooze - 1, distract())

        assertEquals("스누즈가 남아 있는 동안 승급 없음", 1, machine.state.level)
        assert(machine.state.distractSeconds >= policy.levelSeconds[1]) {
            "이미 2단계 임계값은 넘긴 상태여야 테스트가 의미 있다"
        }
    }

    @Test
    fun `스누즈가 끝나면 직전 레벨에서 재개한다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.run(policy.levelSeconds[0], distract())
        machine.snooze(60)

        machine.run(60, distract()) // 스누즈 소진
        assertEquals("레벨은 유지된다", 1, machine.state.level)

        machine.run(policy.levelSeconds[1], distract())
        assertEquals("재개 후 승급", 2, machine.state.level)
    }

    @Test
    fun `스누즈 중에도 누적은 계속된다`() {
        val machine = FocusStateMachine(policy).apply { start() }
        machine.snooze(60)
        machine.run(60, distract())

        assertEquals("스누즈로 시간을 되돌릴 수는 없다", 60, machine.state.distractSeconds)
    }

    @Test
    fun `대기 중에는 스누즈가 걸리지 않는다`() {
        val machine = FocusStateMachine(policy)
        machine.snooze(60)
        assertEquals(0, machine.state.snoozeRemainingSeconds)
    }

    // --- 데모 모드 (#65) ---

    @Test
    fun `데모 모드는 초 단위로 승급한다`() {
        val machine = FocusStateMachine(InterventionPolicy.Demo).apply { start() }
        machine.run(30, Observation(Judgement.DISTRACT, 5))

        assertEquals(1, machine.state.level)
    }

    @Test
    fun `데모 모드가 기본 모드 값을 바꾸지 않는다`() {
        assertEquals(10 * 60, InterventionPolicy().levelSeconds[0])
        assertEquals(30, InterventionPolicy.Demo.levelSeconds[0])
    }
}
