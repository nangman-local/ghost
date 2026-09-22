package com.ghost.prototype.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.prototype.R
import com.ghost.prototype.contract.FocusError
import com.ghost.prototype.contract.Task
import com.ghost.prototype.ui.component.Checkpoint
import com.ghost.prototype.ui.component.FieldButton
import com.ghost.prototype.ui.component.FocusProgress
import com.ghost.prototype.ui.component.GhostCard
import com.ghost.prototype.ui.component.GhostIllustration
import com.ghost.prototype.ui.component.PrimaryButton
import com.ghost.prototype.ui.component.SpeechBubble
import com.ghost.prototype.ui.component.TaskInputField
import com.ghost.prototype.ui.component.TaskRow
import com.ghost.prototype.ui.home.taskpicker.TaskPickerActions
import com.ghost.prototype.ui.home.taskpicker.TaskPickerSheet
import com.ghost.prototype.ui.openOverlaySettings
import com.ghost.prototype.ui.theme.GhostTheme

class HomeActions(
    val onInputChange: (String) -> Unit = {},
    val onAddTask: () -> Unit = {},
    val onStartTask: () -> Unit = {},
    val onStopTask: () -> Unit = {},
    val onOpenTaskPicker: () -> Unit = {},
    val onOpenOverlaySettings: () -> Unit = {},
)

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    HomeScreen(
        state = state,
        actions = HomeActions(
            onInputChange = viewModel::onInputChange,
            onAddTask = viewModel::addTask,
            onStartTask = viewModel::startTask,
            onStopTask = viewModel::stopTask,
            onOpenTaskPicker = viewModel::openPicker,
            onOpenOverlaySettings = { context.openOverlaySettings(viewModel::settingsUnavailable) },
        ),
    )
    state.picker?.let {
        TaskPickerSheet(
            it,
            TaskPickerActions(
                onSelect = viewModel::selectTask,
                onInputChange = viewModel::onPickerInputChange,
                onAdd = viewModel::addTaskFromPicker,
                onDismiss = viewModel::closePicker,
            ),
        )
    }
}

/** 하단 탭은 바깥(MainTabs)이 붙인다. 이 화면은 탭 위 영역만 그린다. */
@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val content = state.content
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.fillMaxSize().widthIn(max = 480.dp).statusBarsPadding()) {
            // 공간이 모자라면 캐릭터 영역이 먼저 줄고, 그래도 모자라면 스크롤된다.
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            ) {
                Box(
                    Modifier.weight(1f).heightIn(min = 140.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box {
                        GhostIllustration(state.pose, Modifier.padding(top = 28.dp))
                        SpeechBubble(
                            stringResource(if (content is HomeContent.NoTask) R.string.home_bubble_no_task else R.string.home_bubble_task),
                            Modifier.align(Alignment.TopStart).offset(x = (-24).dp),
                        )
                    }
                }
                if (content is HomeContent.InProgress) {
                    FocusProgress(content.progress, content.checkpoints, Modifier.padding(bottom = 24.dp))
                }
                state.error?.let { ErrorLine(it, actions.onOpenOverlaySettings) }
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    when (content) {
                        is HomeContent.NoTask -> NoTaskCard(content, actions)
                        is HomeContent.TaskReady -> TaskReadyCard(content, actions)
                        is HomeContent.InProgress -> RecordCard(content, actions)
                    }
                }
            }
        }
    }
}

/** 오류 종류별 문구. 새 종류가 생기면 `when`이 컴파일 오류로 알려준다. */
@StringRes
private fun HomeError.message(): Int = when (this) {
    is HomeError.Focus -> when (error) {
        FocusError.OVERLAY_PERMISSION_MISSING -> R.string.error_permission
        FocusError.OVERLAY_ATTACH_FAILED -> R.string.error_overlay
    }
    HomeError.SettingsUnavailable -> R.string.error_settings
}

/** 권한 문제일 때만 '권한 설정'으로 안내한다. */
private val HomeError.offersOverlaySettings: Boolean
    get() = this == HomeError.Focus(FocusError.OVERLAY_PERMISSION_MISSING)

@Composable
private fun ErrorLine(error: HomeError, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(error.message()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (error.offersOverlaySettings) {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.home_open_overlay_settings), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
}

@Composable
private fun NoTaskCard(content: HomeContent.NoTask, actions: HomeActions) {
    GhostCard(horizontalAlignment = Alignment.CenterHorizontally) {
        CardTitle(stringResource(R.string.home_no_task_title))
        TaskInputField(
            value = content.input,
            onValueChange = actions.onInputChange,
            placeholder = stringResource(R.string.home_task_placeholder),
            onSubmit = actions.onAddTask,
        )
        PrimaryButton(stringResource(R.string.home_add_task), onClick = actions.onAddTask)
    }
}

@Composable
private fun TaskReadyCard(content: HomeContent.TaskReady, actions: HomeActions) {
    GhostCard {
        CardTitle(stringResource(R.string.home_today_title))
        TaskRow(content.task.title, onClick = actions.onOpenTaskPicker)
        PrimaryButton(stringResource(R.string.home_start), onClick = actions.onStartTask)
    }
}

@Composable
private fun RecordCard(content: HomeContent.InProgress, actions: HomeActions) {
    GhostCard {
        CardTitle(stringResource(R.string.home_record_title))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RecordItem(R.drawable.ic_focus_time, stringResource(R.string.home_focus_time), content.focusTime, Modifier.weight(1f))
            RecordItem(R.drawable.ic_distract_time, stringResource(R.string.home_distract_time), content.distractTime, Modifier.weight(1f))
        }
        // Figma의 빈 박스(56:571)를 '그만하기'로 임시 구현. 디자인팀 확인 필요.
        FieldButton(stringResource(R.string.home_stop), onClick = actions.onStopTask)
    }
}

@Composable
private fun RecordItem(icon: Int, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 35.dp, height = 37.dp)
                .background(GhostTheme.colors.surfaceDim, MaterialTheme.shapes.small)
                .border(0.5.dp, GhostTheme.colors.onMuted.copy(alpha = 0.5f), MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private val sampleTask = Task("1", "기말 과제 마감")

@Preview(name = "할 일 없음", widthDp = 360, heightDp = 640)
@Preview(name = "할 일 없음, 작은 화면 1.3배", widthDp = 360, heightDp = 560, fontScale = 1.3f)
@Composable
private fun HomeNoTaskPreview() {
    GhostTheme { HomeScreen(HomeUiState(HomeContent.NoTask("")), HomeActions()) }
}

@Preview(name = "오늘의 할 일", widthDp = 360, heightDp = 640)
@Preview(name = "오늘의 할 일, 폴드", widthDp = 673, heightDp = 760)
@Composable
private fun HomeTaskReadyPreview() {
    GhostTheme { HomeScreen(HomeUiState(HomeContent.TaskReady(sampleTask)), HomeActions()) }
}

@Preview(name = "진행 중", widthDp = 360, heightDp = 640)
@Preview(name = "진행 중, A 시리즈 1.5배", widthDp = 411, heightDp = 780, fontScale = 1.5f)
@Composable
private fun HomeInProgressPreview() {
    GhostTheme {
        HomeScreen(
            HomeUiState(
                HomeContent.InProgress(
                    sampleTask, 0.74f,
                    listOf(Checkpoint(0.1f, "10%", true), Checkpoint(0.3f, "30%", true), Checkpoint(0.5f, "50%", true)),
                    "1:23", "0:34",
                ),
            ),
            HomeActions(),
        )
    }
}

@Preview(name = "오류", widthDp = 360, heightDp = 640)
@Composable
private fun HomeErrorPreview() {
    GhostTheme {
        HomeScreen(
            HomeUiState(HomeContent.TaskReady(sampleTask), error = HomeError.Focus(FocusError.OVERLAY_PERMISSION_MISSING)),
            HomeActions(),
        )
    }
}

