package com.ghost.prototype.focus

import com.ghost.prototype.contract.FocusController
import com.ghost.prototype.contract.FocusControllerContract
import com.ghost.prototype.contract.SessionState
import com.ghost.prototype.detection.AppObservation
import com.ghost.prototype.floating.FloatingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 실제 구현이 `FocusControllerContract`의 모든 동작을 지키는지 확인한다.
 * 상태머신 자체의 규칙은 `FocusStateMachineTest`에서 본다 — 여기는 배선을 본다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GhostFocusControllerTest : FocusControllerContract() {

    override fun TestScope.create(): FocusController = build().controller

    private class Fixture(
        val controller: GhostFocusController,
        val observations: MutableStateFlow<AppObservation?>,
        val floating: MutableStateFlow<FloatingState>,
        val clock: () -> Long,
        val advance: (Int) -> Unit,
        val setScreenOn: (Boolean) -> Unit,
    )

    /** 가상 시계로 돌린다. 실제 시간이 흐르기를 기다리지 않는다. */
    private val policy = InterventionPolicy()

    private fun TestScope.build(
        policy: InterventionPolicy = this@GhostFocusControllerTest.policy,
        overlayGranted: Boolean = true,
    ): Fixture {
        var now = 1_000_000L
        var screenOn = true
        val observations = MutableStateFlow<AppObservation?>(null)
        val floating = MutableStateFlow(FloatingState())

        val controller = GhostFocusController(
            startFloating = { floating.value = FloatingState(visible = true) },
            stopFloating = { floating.value = FloatingState() },
            floating = floating,
            observations = observations,
            judge = RuleJudge.fromSharedRules("com.ghost.prototype"),
            overlayGranted = { overlayGranted },
            scope = backgroundScope,
            policy = policy,
            screenOn = { screenOn },
            nowMillis = { now },
            // 가상 시간으로 미룬다. runTest 가 즉시 진행시키므로 실제로 기다리지 않는다.
            // 가상 시간이 흐른 만큼 시계도 같이 민다 — 둘이 어긋나면 tick 이 elapsed 를 잘못 센다.
            delayMillis = { millis -> delay(millis); now += millis },
        )
        return Fixture(
            controller = controller,
            observations = observations,
            floating = floating,
            clock = { now },
            // 가상 시간을 흘려보내면 tick 루프가 그만큼 돌고 시계도 따라 움직인다.
            advance = { seconds -> testScheduler.advanceTimeBy(seconds * 1000L) },
            setScreenOn = { screenOn = it },
        )
    }

    @Test
    fun `딴짓 앱을 관측하면 누적한다`() = runTest {
        val f = build()
        f.controller.startTask("t1")
        settle()

        f.observations.value = AppObservation("com.instagram.android", "Instagram", f.clock())
        settle()
        f.advance(120)
        settle()

        assert(f.controller.distractSeconds > 0) {
            "딴짓 앱을 관측했으면 누적돼야 한다 (실제: ${f.controller.distractSeconds})"
        }
    }

    @Test
    fun `집중 앱은 누적하지 않는다`() = runTest {
        val f = build()
        f.controller.startTask("t1")
        settle()

        f.observations.value = AppObservation("com.notion.id", "Notion", f.clock())
        settle()
        f.advance(300)
        settle()

        assertEquals(0, f.controller.distractSeconds)
        assertEquals(0, f.controller.state.value.interventionLevel)
    }

    @Test
    fun `모호한 앱은 개입하지 않는다`() = runTest {
        val f = build()
        f.controller.startTask("t1")
        settle()

        // 크롬은 AMBIGUOUS. 서버 /judge(#12) 없이는 판정하지 않는다.
        f.observations.value = AppObservation("com.android.chrome", "Chrome", f.clock())
        settle()
        f.advance(1800)
        settle()

        assertEquals("모호하면 개입하지 않는다", 0, f.controller.state.value.interventionLevel)
    }

    @Test
    fun `누적은 유지 시간만큼 깎이지 않는다`() = runTest {
        val f = build()
        f.controller.startTask("t1")
        settle()

        f.observations.value = AppObservation("com.instagram.android", "Instagram", f.clock())
        settle()
        f.advance(policy.levelSeconds[0]) // 정확히 10분
        settle()

        // minHoldSeconds(20초)가 누적에서 사라지면 9분 40초가 되어 승급하지 않는다.
        // "유튜브 본 지 10분 됐어"는 실제 경과 시간이어야 한다.
        assertEquals("10분 딴짓이면 1단계여야 한다", 1, f.controller.state.value.interventionLevel)
    }

    @Test
    fun `화면이 꺼진 동안은 딴짓이 누적되지 않는다`() = runTest {
        // #70: 인스타그램을 보다가 화면을 끄고 자면, 그 뒤로도 계속 딴짓으로 쌓여서는 안 된다.
        val f = build()
        f.controller.startTask("t1")
        settle()

        f.observations.value = AppObservation("com.instagram.android", "Instagram", f.clock())
        settle()
        f.advance(policy.levelSeconds[0]) // 정확히 10분 — 1단계에 도달시켜 둔다
        settle()
        val beforeSleep = f.controller.distractSeconds
        val levelBeforeSleep = f.controller.state.value.interventionLevel

        f.setScreenOn(false)
        f.advance(8 * 60 * 60) // 8시간 동안 화면이 꺼져 있어도 관측(Instagram)은 그대로 남는다

        assertEquals(
            "화면이 꺼진 동안은 누적이 멈춰야 한다",
            beforeSleep,
            f.controller.distractSeconds,
        )
        assertEquals(
            "화면이 꺼진 동안은 개입 레벨도 오르면 안 된다",
            levelBeforeSleep,
            f.controller.state.value.interventionLevel,
        )
    }

    @Test
    fun `화면을 다시 켜면 꺼져 있던 시간을 몰아서 누적하지 않고 이어간다`() = runTest {
        val f = build()
        f.controller.startTask("t1")
        settle()

        f.observations.value = AppObservation("com.instagram.android", "Instagram", f.clock())
        settle()
        f.advance(30)
        settle()
        val beforeSleep = f.controller.distractSeconds

        f.setScreenOn(false)
        f.advance(8 * 60 * 60)
        f.setScreenOn(true)
        settle()

        // 핵심 확인: 꺼져 있던 8시간(28800초)이 한꺼번에 반영되면 안 된다.
        // 틱 경계에서 1~2초 오차는 날 수 있으나(가상 시계 스케줄링), 8시간 전체가 새면 이 값이 실패한다.
        val afterWake = f.controller.distractSeconds
        assert(afterWake - beforeSleep < 10) {
            "화면을 켠 직후 누적이 크게 뛰었다 — 꺼져 있던 시간이 몰아서 반영된 것으로 보인다 " +
                "(자기 전: $beforeSleep, 깬 후: $afterWake)"
        }

        f.advance(60)
        settle()
        assert(f.controller.distractSeconds > afterWake) {
            "화면을 켠 뒤에는 다시 정상적으로 누적돼야 한다 (실제: ${f.controller.distractSeconds})"
        }
    }

    @Test
    fun `종료하면 누적이 사라진다`() = runTest {
        val f = build()
        f.controller.startTask("t1")
        settle()
        f.observations.value = AppObservation("com.instagram.android", "Instagram", f.clock())
        settle()
        f.advance(300)
        settle()

        f.controller.stop()
        settle()

        assertEquals(0, f.controller.distractSeconds)
        assertEquals(SessionState.WAITING, f.controller.state.value.state)
    }
}
