package com.ghost.prototype.ui.onboarding

import com.ghost.prototype.contract.Permission

/** Figma 온보딩 5개 프레임 순서. 권한 단계만 인디케이터(4칸)에 나온다. */
enum class OnboardingStep(val permission: Permission?) {
    WELCOME(null),
    OVERLAY(Permission.OVERLAY),
    ACCESSIBILITY(Permission.ACCESSIBILITY),
    USAGE(Permission.USAGE_ACCESS),
    NOTIFICATIONS(Permission.NOTIFICATIONS),
    ;

    /** 인디케이터 위치. 시작 화면은 -1. */
    val indicatorIndex: Int get() = ordinal - 1

    companion object {
        val INDICATOR_COUNT = entries.count { it.permission != null }
    }
}

/**
 * 단계 전환 규칙(순수 로직). 이미 허용된 권한 단계와 이 기기에서 해당 없는 단계는 건너뛴다.
 * 권한이 없어도 "나중에"로 다음 단계에 갈 수 있다 — 플로팅·앱 사용에 권한을 강제하지 않는다.
 */
class OnboardingFlow(
    private val isGranted: (Permission) -> Boolean,
    /** 알림 런타임 권한은 Android 13(API 33)부터다. 그 전에는 단계 자체가 없다. */
    private val notificationsRequestable: Boolean,
) {
    fun isNeeded(step: OnboardingStep): Boolean {
        val permission = step.permission ?: return true
        if (permission == Permission.NOTIFICATIONS && !notificationsRequestable) return false
        return !isGranted(permission)
    }

    /** 다음 단계. null이면 온보딩이 끝난 것이다. */
    fun next(from: OnboardingStep): OnboardingStep? =
        OnboardingStep.entries.drop(from.ordinal + 1).firstOrNull(::isNeeded)

    /** 이전 단계. 허용된 단계로 돌아가면 곧바로 다시 넘어가므로 건너뛴다. */
    fun previous(from: OnboardingStep): OnboardingStep? =
        OnboardingStep.entries.take(from.ordinal).lastOrNull(::isNeeded)

    /** 설정에서 돌아왔을 때: 지금 단계의 권한이 허용됐으면 다음으로 간다. */
    fun afterResume(current: OnboardingStep): OnboardingStep? =
        if (isNeeded(current)) current else next(current)
}
