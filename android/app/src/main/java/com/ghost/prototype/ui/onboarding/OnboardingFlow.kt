package com.ghost.prototype.ui.onboarding

import com.ghost.prototype.contract.Permission

/**
 * 단계 전환 규칙(순수 로직). 단계 목록은 [OnboardingSteps.all]에서 받는다.
 * 이미 허용된 권한 단계와 이 기기에서 해당 없는 단계는 건너뛴다.
 * 권한이 없어도 "나중에"로 다음 단계에 갈 수 있다 — 플로팅·앱 사용에 권한을 강제하지 않는다.
 */
class OnboardingFlow(
    steps: List<OnboardingStep>,
    private val isGranted: (Permission) -> Boolean,
    sdkInt: Int,
) {
    /** 이 기기에 있는 단계. 런타임 권한 단계는 [GrantMethod.RuntimePermission.minSdk] 이상에서만 있다. */
    val steps: List<OnboardingStep> = steps.filter { step ->
        val runtime = step.grant as? GrantMethod.RuntimePermission
        runtime == null || sdkInt >= runtime.minSdk
    }

    /** 인디케이터에 나오는 단계 = 권한 단계. */
    private val permissionSteps = this.steps.filter { it.permission != null }

    val first: OnboardingStep get() = steps.first()

    val indicatorCount: Int get() = permissionSteps.size

    /** 인디케이터 위치. 권한 없는 안내 단계는 -1. */
    fun indicatorIndex(step: OnboardingStep): Int = permissionSteps.indexOf(step)

    fun stepFor(key: String?): OnboardingStep? = steps.firstOrNull { it.key == key }

    fun isNeeded(step: OnboardingStep): Boolean {
        val permission = step.permission ?: return true
        return !isGranted(permission)
    }

    /** 다음 단계. null이면 온보딩이 끝난 것이다. */
    fun next(from: OnboardingStep): OnboardingStep? =
        steps.drop(steps.indexOf(from) + 1).firstOrNull(::isNeeded)

    /** 이전 단계. 허용된 단계로 돌아가면 곧바로 다시 넘어가므로 건너뛴다. */
    fun previous(from: OnboardingStep): OnboardingStep? =
        steps.take(steps.indexOf(from).coerceAtLeast(0)).lastOrNull(::isNeeded)

    /** 설정에서 돌아왔을 때: 지금 단계의 권한이 허용됐으면 다음으로 간다. */
    fun afterResume(current: OnboardingStep): OnboardingStep? =
        if (isNeeded(current)) current else next(current)
}
