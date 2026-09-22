package com.ghost.prototype.ui.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R
import com.ghost.prototype.contract.Permission
import com.ghost.prototype.ui.component.GhostPose
import com.ghost.prototype.ui.openAccessibilitySettings
import com.ghost.prototype.ui.openAppNotificationSettings
import com.ghost.prototype.ui.openOverlaySettings
import com.ghost.prototype.ui.openUsageSettings

/** 권한을 받는 방법. 새 권한 단계는 둘 중 하나를 고른다. */
sealed interface GrantMethod {
    /** 시스템 설정 화면을 연다. 돌아왔을 때(onResume) 허용됐으면 다음 단계로 간다. 예: 오버레이·접근성·배터리 최적화. */
    class OpenSettings(val open: Context.(onUnavailable: () -> Unit) -> Unit) : GrantMethod

    /**
     * 런타임 권한 팝업. [minSdk] 미만 기기에는 이 단계가 없다.
     * 두 번 거부돼 시스템이 더 이상 팝업을 띄우지 않으면 [whenBlocked] 설정 화면을 연다.
     */
    class RuntimePermission(
        val permission: String,
        val minSdk: Int,
        val whenBlocked: Context.(onUnavailable: () -> Unit) -> Unit,
    ) : GrantMethod
}

sealed interface OnboardingArt {
    data class Ghost(val pose: GhostPose) : OnboardingArt
    data class Picture(@DrawableRes val drawable: Int, val width: Dp, val height: Dp) : OnboardingArt
}

/**
 * 온보딩 한 단계(화면 한 장). [permission]이 null이면 권한 없는 안내 단계로,
 * 인디케이터에 나오지 않고 "나중에"도 없다.
 */
class OnboardingStep(
    /** 화면 회전·프로세스 복원 때 현재 단계를 기억하는 키. 단계마다 달라야 한다. */
    val key: String,
    @StringRes val title: Int,
    @StringRes val body: Int? = null,
    val art: OnboardingArt,
    @StringRes val action: Int,
    val permission: Permission? = null,
    val grant: GrantMethod? = null,
)

/**
 * 온보딩 단계 표. Figma 온보딩 프레임(50:3101 · 50:3120 · 50:3171 · 50:3198 · 50:3231) 순서다.
 *
 * **새 권한 단계 추가 방법** (예: 배터리 최적화 예외)
 * 1. `contract/Permission`에 값을 추가하고 `permission/AndroidPermissionStatus`에 허용 여부 확인을 넣는다.
 * 2. 아래에 [OnboardingStep]을 하나 만들고 [all]의 원하는 위치에 끼워 넣는다. 문구는 `strings.xml`에 둔다.
 * 인디케이터 개수, 건너뛰기, 자동 진행, 뒤로가기는 [OnboardingFlow]가 알아서 맞춘다.
 *
 * Play 정책: 접근성·사용 정보 접근은 요청 "전에" 무엇을 읽고 무엇을 읽지 않는지 눈에 띄게 고지해야 한다.
 * 지금 문구는 Figma를 그대로 따른 것이며 이 요구를 충족하지 않는다. 공개 배포 전 #23에서 문구를 정한다.
 */
object OnboardingSteps {
    val WELCOME = OnboardingStep(
        key = "welcome",
        title = R.string.onboarding_welcome_title,
        art = OnboardingArt.Ghost(GhostPose.DEFAULT),
        action = R.string.onboarding_welcome_action,
    )

    val OVERLAY = OnboardingStep(
        key = "overlay",
        title = R.string.onboarding_overlay_title,
        art = OnboardingArt.Picture(R.drawable.onboarding_overlay, 268.dp, 250.dp),
        action = R.string.onboarding_overlay_action,
        permission = Permission.OVERLAY,
        grant = GrantMethod.OpenSettings { openOverlaySettings(it) },
    )

    val ACCESSIBILITY = OnboardingStep(
        key = "accessibility",
        title = R.string.onboarding_accessibility_title,
        body = R.string.onboarding_accessibility_body,
        art = OnboardingArt.Ghost(GhostPose.CURIOUS),
        action = R.string.onboarding_accessibility_action,
        permission = Permission.ACCESSIBILITY,
        grant = GrantMethod.OpenSettings { openAccessibilitySettings(it) },
    )

    val USAGE = OnboardingStep(
        key = "usage",
        title = R.string.onboarding_usage_title,
        art = OnboardingArt.Ghost(GhostPose.FOCUS),
        action = R.string.onboarding_usage_action,
        permission = Permission.USAGE_ACCESS,
        grant = GrantMethod.OpenSettings { openUsageSettings(it) },
    )

    // POST_NOTIFICATIONS 상수는 API 33부터지만, minSdk 33 미만 기기에는 이 단계 자체가 없다.
    @SuppressLint("InlinedApi")
    val NOTIFICATIONS = OnboardingStep(
        key = "notifications",
        title = R.string.onboarding_notification_title,
        body = R.string.onboarding_notification_body,
        art = OnboardingArt.Ghost(GhostPose.ALERT),
        action = R.string.onboarding_notification_action,
        permission = Permission.NOTIFICATIONS,
        grant = GrantMethod.RuntimePermission(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            minSdk = Build.VERSION_CODES.TIRAMISU,
            whenBlocked = { openAppNotificationSettings(it) },
        ),
    )

    /** 화면 순서. */
    val all: List<OnboardingStep> = listOf(WELCOME, OVERLAY, ACCESSIBILITY, USAGE, NOTIFICATIONS)
}
