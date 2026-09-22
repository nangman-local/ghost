package com.ghost.prototype.ui.home

import com.ghost.prototype.contract.FocusError
import com.ghost.prototype.contract.FocusStats
import com.ghost.prototype.contract.SessionState
import com.ghost.prototype.contract.Task
import com.ghost.prototype.ui.component.Checkpoint
import com.ghost.prototype.ui.component.GhostPose
import com.ghost.prototype.ui.home.taskpicker.TaskPickerUiState
import java.util.Locale

/**
 * 홈 3상태(Figma 47:385 · 50:2160 · 56:521).
 * 전환: NoTask →(추가) TaskReady →(시작) InProgress →(그만하기) TaskReady
 */
sealed interface HomeContent {
    data class NoTask(val input: String) : HomeContent
    data class TaskReady(val task: Task) : HomeContent
    data class InProgress(
        val task: Task,
        val progress: Float,
        val checkpoints: List<Checkpoint>,
        /** 오늘 누적, `H:MM`. */
        val focusTime: String,
        val distractTime: String,
    ) : HomeContent
}

/** 홈에 보여줄 오류의 종류. 문구와 버튼은 화면이 종류별로 정한다. 자동 재시도는 하지 않는다. */
sealed interface HomeError {
    /** 세션·플로팅 실패 (코어가 contract로 알려준 종류). */
    data class Focus(val error: FocusError) : HomeError

    /** 시스템 설정 화면을 열지 못했다. */
    data object SettingsUnavailable : HomeError
}

data class HomeUiState(
    val content: HomeContent = HomeContent.NoTask(""),
    val error: HomeError? = null,
    /** null이면 할 일 팝업이 닫혀 있다. */
    val picker: TaskPickerUiState? = null,
) {
    val pose: GhostPose
        get() = if (content is HomeContent.NoTask) GhostPose.PEEK else GhostPose.FOCUS
}

/** 오늘 누적 초 → `H:MM` (예: 83분 → "1:23"). */
fun formatHoursMinutes(seconds: Long): String {
    val totalMinutes = seconds.coerceAtLeast(0) / 60
    return String.format(Locale.ROOT, "%d:%02d", totalMinutes / 60, totalMinutes % 60)
}

/** contract 값 → 홈 화면 상태. 순수 함수라 단위 테스트한다. */
internal fun homeContent(
    currentTask: Task?,
    runningTaskId: String?,
    sessionState: SessionState,
    tasks: List<Task>,
    stats: FocusStats,
    input: String,
): HomeContent {
    val running = runningTaskId?.takeIf { sessionState != SessionState.WAITING }
        ?.let { id -> tasks.firstOrNull { it.id == id } }
    return when {
        running != null -> HomeContent.InProgress(
            task = running,
            // TODO(디자인·서버 합의): 진행률과 체크포인트(10/30/50%)의 의미가 정해지면 contract에서 받는다.
            progress = PLACEHOLDER_PROGRESS,
            checkpoints = PLACEHOLDER_CHECKPOINTS,
            focusTime = formatHoursMinutes(stats.focusSeconds),
            distractTime = formatHoursMinutes(stats.distractSeconds),
        )
        currentTask != null -> HomeContent.TaskReady(currentTask)
        else -> HomeContent.NoTask(input)
    }
}

private const val PLACEHOLDER_PROGRESS = 0.74f
private val PLACEHOLDER_CHECKPOINTS = listOf(
    Checkpoint(0.1f, "10%", done = true),
    Checkpoint(0.3f, "30%", done = true),
    Checkpoint(0.5f, "50%", done = true),
)
