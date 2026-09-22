package com.ghost.prototype.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.prototype.R
import com.ghost.prototype.ui.component.GhostIllustration
import com.ghost.prototype.ui.component.GhostPose
import com.ghost.prototype.ui.component.PageIndicator
import com.ghost.prototype.ui.component.PrimaryButton
import com.ghost.prototype.ui.component.SecondaryTextButton
import com.ghost.prototype.ui.openAccessibilitySettings
import com.ghost.prototype.ui.openAppNotificationSettings
import com.ghost.prototype.ui.openOverlaySettings
import com.ghost.prototype.ui.openUsageSettings
import com.ghost.prototype.ui.theme.GhostTheme

/** 단계별 화면 내용. Figma 온보딩 프레임(50:3101 · 50:3120 · 50:3171 · 50:3198 · 50:3231). */
private sealed interface Art {
    data class Ghost(val pose: GhostPose) : Art
    data class Picture(@DrawableRes val drawable: Int) : Art
}

private data class StepContent(
    @StringRes val title: Int,
    @StringRes val body: Int?,
    val art: Art,
    @StringRes val action: Int,
)

// Play 정책: 접근성·사용 정보 접근은 요청 "전에" 무엇을 읽고 무엇을 읽지 않는지 눈에 띄게 고지해야 한다.
// 지금 문구는 Figma를 그대로 따른 것이며 이 요구를 충족하지 않는다. 공개 배포 전 #23에서 문구를 정한다.
private fun OnboardingStep.content() = when (this) {
    OnboardingStep.WELCOME -> StepContent(
        R.string.onboarding_welcome_title, null, Art.Ghost(GhostPose.DEFAULT), R.string.onboarding_welcome_action,
    )
    OnboardingStep.OVERLAY -> StepContent(
        R.string.onboarding_overlay_title, null, Art.Picture(R.drawable.onboarding_overlay), R.string.onboarding_overlay_action,
    )
    OnboardingStep.ACCESSIBILITY -> StepContent(
        R.string.onboarding_accessibility_title, R.string.onboarding_accessibility_body,
        Art.Ghost(GhostPose.CURIOUS), R.string.onboarding_accessibility_action,
    )
    OnboardingStep.USAGE -> StepContent(
        R.string.onboarding_usage_title, null, Art.Ghost(GhostPose.FOCUS), R.string.onboarding_usage_action,
    )
    OnboardingStep.NOTIFICATIONS -> StepContent(
        R.string.onboarding_notification_title, R.string.onboarding_notification_body,
        Art.Ghost(GhostPose.ALERT), R.string.onboarding_notification_action,
    )
}

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    BackHandler(enabled = state.canGoBack) { viewModel.back() }

    val unavailable = viewModel::settingsUnavailable
    OnboardingScreen(
        state = state,
        onAction = {
            when (state.step) {
                OnboardingStep.WELCOME -> viewModel.start()
                OnboardingStep.OVERLAY -> context.openOverlaySettings(unavailable)
                OnboardingStep.ACCESSIBILITY -> context.openAccessibilitySettings(unavailable)
                OnboardingStep.USAGE -> context.openUsageSettings(unavailable)
                OnboardingStep.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    // 한 번 물어본 뒤 rationale도 false면 시스템이 더 이상 팝업을 띄우지 않는다 → 설정으로 보낸다.
                    val blocked = viewModel.prefs.notificationAsked &&
                        activity?.shouldShowRequestPermissionRationale(permission) == false
                    if (blocked) {
                        context.openAppNotificationSettings(unavailable)
                    } else {
                        viewModel.prefs.notificationAsked = true
                        notificationLauncher.launch(permission)
                    }
                }
            }
        },
        onSkip = viewModel::skip,
        onBack = viewModel::back,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val content = state.step.content()
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = 480.dp)) {
            // 상단: 뒤로가기 (시작 화면에는 돌아갈 곳이 없어 숨긴다)
            Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
                if (state.canGoBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            // 가운데: 공간이 모자라면 그림이 먼저 줄고, 그래도 모자라면 스크롤된다
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight).padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(0.2f).heightIn(min = 24.dp))
                    Text(
                        stringResource(content.title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    content.body?.let {
                        Text(
                            stringResource(it),
                            style = MaterialTheme.typography.bodyLarge,
                            color = GhostTheme.colors.onMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Box(
                        Modifier.weight(1f).heightIn(min = 120.dp).fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (val art = content.art) {
                            is Art.Ghost -> GhostIllustration(art.pose)
                            is Art.Picture -> Image(
                                painterResource(art.drawable),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.sizeIn(maxWidth = 268.dp, maxHeight = 250.dp)
                                    .aspectRatio(268f / 250f).clip(RoundedCornerShape(16.dp)),
                            )
                        }
                    }
                    state.error?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
            }
            // 하단: 인디케이터와 버튼은 항상 보인다
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.step.indicatorIndex >= 0) {
                    PageIndicator(OnboardingStep.INDICATOR_COUNT, state.step.indicatorIndex)
                }
                PrimaryButton(stringResource(content.action), onClick = onAction)
                if (state.step.permission != null) {
                    SecondaryTextButton(stringResource(R.string.onboarding_later), onClick = onSkip)
                }
            }
        }
    }
}

@Preview(name = "S25", widthDp = 360, heightDp = 780)
@Preview(name = "작은 16:9, 글자 1.3배", widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Preview(name = "A 시리즈", widthDp = 411, heightDp = 891)
@Preview(name = "폴드 펼침", widthDp = 673, heightDp = 841)
@Composable
private fun OnboardingOverlayPreview() {
    GhostTheme { OnboardingScreen(OnboardingUiState(OnboardingStep.OVERLAY, canGoBack = true), {}, {}, {}) }
}

@Preview(name = "알림, 글자 1.5배", widthDp = 360, heightDp = 640, fontScale = 1.5f)
@Preview(name = "알림", widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingNotificationPreview() {
    GhostTheme { OnboardingScreen(OnboardingUiState(OnboardingStep.NOTIFICATIONS, canGoBack = true), {}, {}, {}) }
}

@Preview(name = "시작", widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingWelcomePreview() {
    GhostTheme { OnboardingScreen(OnboardingUiState(), {}, {}, {}) }
}
