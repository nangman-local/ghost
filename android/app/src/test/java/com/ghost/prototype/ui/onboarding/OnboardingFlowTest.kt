package com.ghost.prototype.ui.onboarding

import com.ghost.prototype.contract.Permission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingFlowTest {
    private fun flow(vararg granted: Permission, notifications: Boolean = true) =
        OnboardingFlow(isGranted = { it in granted }, notificationsRequestable = notifications)

    @Test
    fun walksAllStepsWhenNothingGranted() {
        val flow = flow()
        val visited = generateSequence(OnboardingStep.WELCOME) { flow.next(it) }.toList()
        assertEquals(OnboardingStep.entries, visited)
    }

    @Test
    fun skipsGrantedSteps() {
        val flow = flow(Permission.OVERLAY, Permission.USAGE_ACCESS)
        assertEquals(OnboardingStep.ACCESSIBILITY, flow.next(OnboardingStep.WELCOME))
        assertEquals(OnboardingStep.NOTIFICATIONS, flow.next(OnboardingStep.ACCESSIBILITY))
    }

    @Test
    fun notificationStepDoesNotExistBeforeAndroid13() {
        val flow = flow(notifications = false)
        assertNull(flow.next(OnboardingStep.USAGE))
    }

    @Test
    fun finishesWhenLastStepGranted() {
        assertNull(flow(Permission.NOTIFICATIONS).afterResume(OnboardingStep.NOTIFICATIONS))
    }

    @Test
    fun staysWhenPermissionStillMissingAfterResume() {
        assertEquals(OnboardingStep.OVERLAY, flow().afterResume(OnboardingStep.OVERLAY))
    }

    @Test
    fun advancesWhenPermissionGrantedAfterResume() {
        assertEquals(OnboardingStep.ACCESSIBILITY, flow(Permission.OVERLAY).afterResume(OnboardingStep.OVERLAY))
    }

    @Test
    fun previousSkipsGrantedStepsButKeepsWelcome() {
        val flow = flow(Permission.OVERLAY, Permission.ACCESSIBILITY)
        assertEquals(OnboardingStep.WELCOME, flow.previous(OnboardingStep.USAGE))
        assertNull(flow.previous(OnboardingStep.WELCOME))
    }
}
