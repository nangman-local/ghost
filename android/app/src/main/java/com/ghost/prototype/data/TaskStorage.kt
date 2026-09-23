package com.ghost.prototype.data

import com.ghost.prototype.contract.Task
import com.ghost.prototype.contract.TaskSource

/**
 * 할 일 저장소의 읽기·쓰기 경계. Android 의존이 없어 단위 테스트에서 가짜로 바꿔 끼운다
 * (`android/AGENTS.md`: Android 독립 로직은 순수 Kotlin으로 분리한다).
 */
interface TaskStorage {
    fun load(): StoredTasks
    fun save(value: StoredTasks)
}

/**
 * 기기에 저장하는 내용. **개인 식별 정보를 넣지 않는다**(#53).
 * 할 일 제목은 사용자가 직접 쓴 것이라 기기 밖으로 내보내지 않는다.
 *
 * @param statsDate 누적이 어느 날짜 것인지. 날짜가 바뀌면 '오늘 누적'을 0으로 되돌린다.
 */
data class StoredTasks(
    val tasks: List<Task> = emptyList(),
    val currentId: String? = null,
    val nextId: Int = 0,
    val focusSeconds: Long = 0,
    val distractSeconds: Long = 0,
    val statsDate: String = "",
)

/**
 * 저장된 값을 오늘 기준으로 정리한다. **날짜가 바뀌면 누적만 버리고 할 일은 남긴다.**
 *
 * 순수 함수라 단위 테스트로 검증한다.
 */
fun StoredTasks.forDate(today: String): StoredTasks =
    if (statsDate == today) this
    else copy(focusSeconds = 0, distractSeconds = 0, statsDate = today)

/** 저장에서 읽은 값이 깨져 있어도 앱이 죽지 않게 한다. */
fun sanitize(
    tasks: List<Task>,
    currentId: String?,
    nextId: Int,
): Triple<List<Task>, String?, Int> {
    val valid = tasks.filter { it.id.isNotBlank() && it.title.isNotBlank() }
    val current = currentId?.takeIf { id -> valid.any { it.id == id } }
    // nextId가 작으면 이미 쓴 id와 부딪힌다. 저장값이 깨졌어도 새 id는 항상 비어 있어야 한다.
    val safeNext = maxOf(nextId, valid.size)
    return Triple(valid, current, safeNext)
}

/** 로컬에서 추가한 할 일은 항상 `MANUAL`이다(`shared/api-schema.md`의 `task.source`). */
fun localTask(id: String, title: String): Task = Task(id, title, TaskSource.MANUAL)
