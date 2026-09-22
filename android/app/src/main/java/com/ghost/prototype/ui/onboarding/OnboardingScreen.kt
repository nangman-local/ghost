package com.ghost.prototype.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ghost.prototype.ui.component.PageIndicator
import com.ghost.prototype.ui.component.PrimaryButton
import com.ghost.prototype.ui.component.SecondaryTextButton
import com.ghost.prototype.ui.theme.GhostTheme

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }

    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    BackHandler(enabled = state.canGoBack) { viewModel.back() }

    val unavailable = viewModel::settingsUnavailable
    OnboardingScreen(
        state = state,
        onAction = {
            // 권한 받는 방법은 단계 표(OnboardingSteps)가 정한다. 여기에는 단계별 분기가 없다.
            when (val grant = state.step.grant) {
                null -> viewModel.next()
                is GrantMethod.OpenSettings -> grant.open(context, unavailable)
                is GrantMethod.RuntimePermission -> {
                    // 한 번 물어본 뒤 rationale도 false면 시스템이 더 이상 팝업을 띄우지 않는다 → 설정으로 보낸다.
                    val blocked = viewModel.prefs.wasAsked(grant.permission) &&
                        activity?.shouldShowRequestPermissionRationale(grant.permission) == false
                    if (blocked) {
                        grant.whenBlocked(context, unavailable)
                    } else {
                        viewModel.prefs.markAsked(grant.permission)
                        permissionLauncher.launch(grant.permission)
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
    val step = state.step
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
                        stringResource(step.title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    step.body?.let {
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
                        when (val art = step.art) {
                            is OnboardingArt.Ghost -> GhostIllustration(art.pose)
                            is OnboardingArt.Picture -> Image(
                                painterResource(art.drawable),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.sizeIn(maxWidth = art.width, maxHeight = art.height)
                                    .aspectRatio(art.width / art.height).clip(RoundedCornerShape(16.dp)),
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
                if (state.indicatorIndex >= 0) {
                    PageIndicator(state.indicatorCount, state.indicatorIndex)
                }
                PrimaryButton(stringResource(step.action), onClick = onAction)
                if (step.permission != null) {
                    SecondaryTextButton(stringResource(R.string.onboarding_later), onClick = onSkip)
                }
            }
        }
    }
}

private fun previewState(step: OnboardingStep) = OnboardingUiState(
    step = step,
    canGoBack = step != OnboardingSteps.WELCOME,
    indicatorIndex = OnboardingSteps.all.filter { it.permission != null }.indexOf(step),
    indicatorCount = OnboardingSteps.all.count { it.permission != null },
)

@Preview(name = "S25", widthDp = 360, heightDp = 780)
@Preview(name = "작은 16:9, 글자 1.3배", widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Preview(name = "A 시리즈", widthDp = 411, heightDp = 891)
@Preview(name = "폴드 펼침", widthDp = 673, heightDp = 841)
@Composable
private fun OnboardingOverlayPreview() {
    GhostTheme { OnboardingScreen(previewState(OnboardingSteps.OVERLAY), {}, {}, {}) }
}

@Preview(name = "알림, 글자 1.5배", widthDp = 360, heightDp = 640, fontScale = 1.5f)
@Preview(name = "알림", widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingNotificationPreview() {
    GhostTheme { OnboardingScreen(previewState(OnboardingSteps.NOTIFICATIONS), {}, {}, {}) }
}

@Preview(name = "시작", widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingWelcomePreview() {
    GhostTheme { OnboardingScreen(previewState(OnboardingSteps.WELCOME), {}, {}, {}) }
}
