package com.ghost.prototype.contract

import kotlinx.coroutines.flow.Flow

/** `shared/api-schema.md`의 `task.source`. */
enum class TaskSource { CALENDAR, NOTION, MANUAL }

/** `shared/data-model.md`의 Task 중 UI가 쓰는 필드만. */
data class Task(val id: String, val title: String, val source: TaskSource = TaskSource.MANUAL)

/** 오늘 누적 시간. */
data class FocusStats(val focusSeconds: Long = 0, val distractSeconds: Long = 0)

/**
 * UI ↔ 서버 데이터 경계. 구현은 서버·동기화 담당이 한다.
 * 현재 할 일은 서버 세션이 진실의 원천이므로 [selectTask]의 결과는 [currentTask]로 돌아온다.
 */
interface TaskRepository {
    val tasks: Flow<List<Task>>
    val currentTask: Flow<Task?>
    val todayStats: Flow<FocusStats>

    /** 할 일을 추가하고 바로 현재 할 일로 선택한다. */
    suspend fun addTask(title: String): Task

    suspend fun selectTask(id: String)
}
