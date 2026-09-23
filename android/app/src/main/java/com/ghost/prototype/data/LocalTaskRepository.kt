package com.ghost.prototype.data

import com.ghost.prototype.contract.FocusStats
import com.ghost.prototype.contract.Task
import com.ghost.prototype.contract.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 기기에 저장하는 `TaskRepository`(#53). 앱을 껐다 켜도 할 일이 남는다.
 *
 * **서버가 진실의 원천이라는 원칙은 그대로다**(루트 `AGENTS.md`).
 * 이 구현은 서버 연동(#14) 전까지의 임시 보관이고, 연동 후에는 오프라인 캐시가 된다.
 *
 * 저장 형식은 [TaskStorage]가 정한다. 여기서는 Android를 모른다.
 *
 * @param today 오늘 날짜 문자열. 날짜가 바뀌면 '오늘 누적'을 0으로 되돌린다.
 */
class LocalTaskRepository(
    private val storage: TaskStorage,
    private val today: () -> String,
) : TaskRepository {

    private val stored = MutableStateFlow(load())

    private fun load(): StoredTasks {
        val raw = storage.load().forDate(today())
        val (tasks, currentId, nextId) = sanitize(raw.tasks, raw.currentId, raw.nextId)
        return raw.copy(tasks = tasks, currentId = currentId, nextId = nextId)
    }

    override val tasks: Flow<List<Task>> = stored.map { it.tasks }

    override val currentTask: Flow<Task?> =
        stored.map { snapshot -> snapshot.tasks.firstOrNull { it.id == snapshot.currentId } }

    override val todayStats: Flow<FocusStats> =
        stored.map { FocusStats(it.focusSeconds, it.distractSeconds) }

    override suspend fun addTask(title: String): Task {
        val snapshot = stored.value
        val task = localTask("local_${snapshot.nextId}", title.trim())
        update(
            snapshot.copy(
                tasks = snapshot.tasks + task,
                currentId = task.id,
                nextId = snapshot.nextId + 1,
            ),
        )
        return task
    }

    override suspend fun selectTask(id: String) {
        val snapshot = stored.value
        if (snapshot.tasks.none { it.id == id }) return
        update(snapshot.copy(currentId = id))
    }

    /** 날짜가 바뀐 채로 앱이 켜져 있을 수 있어, 쓸 때마다 오늘 날짜로 맞춘다. */
    private fun update(next: StoredTasks) {
        val dated = next.forDate(today()).copy(statsDate = today())
        stored.value = dated
        storage.save(dated)
    }
}
