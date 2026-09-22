package com.ghost.prototype.ui.onboarding

import com.ghost.prototype.R
import com.ghost.prototype.contract.Permission
import com.ghost.prototype.ui.component.GhostPose
import com.ghost.prototype.ui.onboarding.OnboardingSteps.ACCESSIBILITY
import com.ghost.prototype.ui.onboarding.OnboardingSteps.NOTIFICATIONS
import com.ghost.prototype.ui.onboarding.OnboardingSteps.OVERLAY
import com.ghost.prototype.ui.onboarding.OnboardingSteps.USAGE
import com.ghost.prototype.ui.onboarding.OnboardingSteps.WELCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingFlowTest {
    private fun flow(vararg granted: Permission, sdk: Int = 34, steps: List<OnboardingStep> = OnboardingSteps.all) =
        OnboardingFlow(steps, isGranted = { it in granted }, sdkInt = sdk)

    @Test
    fun walksAllStepsWhenNothingGranted() {
        val flow = flow()
        val visited = generateSequence(flow.first) { flow.next(it) }.toList()
        assertEquals(OnboardingSteps.all, visited)
    }

    @Test
    fun skipsGrantedSteps() {
        val flow = flow(Permission.OVERLAY, Permission.USAGE_ACCESS)
        assertEquals(ACCESSIBILITY, flow.next(WELCOME))
        assertEquals(NOTIFICATIONS, flow.next(ACCESSIBILITY))
    }

    @Test
    fun runtimeStepDoesNotExistBelowItsMinSdk() {
        val flow = flow(sdk = 32)
        assertNull(flow.next(USAGE))
        assertEquals(3, flow.indicatorCount)
    }

    @Test
    fun indicatorCountsOnlyPermissionSteps() {
        val flow = flow()
        assertEquals(4, flow.indicatorCount)
        assertEquals(-1, flow.indicatorIndex(WELCOME))
        assertEquals(0, flow.indicatorIndex(OVERLAY))
        assertEquals(3, flow.indicatorIndex(NOTIFICATIONS))
    }

    @Test
    fun finishesWhenLastStepGranted() {
        assertNull(flow(Permission.NOTIFICATIONS).afterResume(NOTIFICATIONS))
    }

    @Test
    fun staysWhenPermissionStillMissingAfterResume() {
        assertEquals(OVERLAY, flow().afterResume(OVERLAY))
    }

    @Test
    fun advancesWhenPermissionGrantedAfterResume() {
        assertEquals(ACCESSIBILITY, flow(Permission.OVERLAY).afterResume(OVERLAY))
    }

    @Test
    fun previousSkipsGrantedStepsButKeepsWelcome() {
        val flow = flow(Permission.OVERLAY, Permission.ACCESSIBILITY)
        assertEquals(WELCOME, flow.previous(USAGE))
        assertNull(flow.previous(WELCOME))
    }

    @Test
    fun restoresStepByKey() {
        assertEquals(USAGE, flow().stepFor("usage"))
        assertNull(flow(sdk = 32).stepFor("notifications"))
    }

    /** 새 단계는 표에 한 줄 넣는 것만으로 흐름·인디케이터에 반영된다. */
    @Test
    fun addedStepJoinsFlowAndIndicator() {
        val extra = OnboardingStep(
            key = "extra",
            title = R.string.onboarding_usage_title,
            art = OnboardingArt.Ghost(GhostPose.DEFAULT),
            action = R.string.onboarding_usage_action,
            permission = Permission.USAGE_ACCESS,
            grant = GrantMethod.OpenSettings { },
        )
        val flow = flow(Permission.OVERLAY, Permission.ACCESSIBILITY, steps = OnboardingSteps.all + extra)
        assertEquals(5, flow.indicatorCount)
        assertEquals(4, flow.indicatorIndex(extra))
        assertEquals(extra, flow.next(NOTIFICATIONS))
    }
}
