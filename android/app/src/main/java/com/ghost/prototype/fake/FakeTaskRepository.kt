package com.ghost.prototype.fake

import com.ghost.prototype.contract.FocusStats
import com.ghost.prototype.contract.Task
import com.ghost.prototype.contract.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/** 서버 연동 전 임시 구현. 메모리에만 두며 프로세스가 끝나면 사라진다. */
class FakeTaskRepository(
    initialTasks: List<Task> = emptyList(),
    stats: FocusStats = FocusStats(),
) : TaskRepository {
    private val taskList = MutableStateFlow(initialTasks)
    private val currentId = MutableStateFlow(initialTasks.firstOrNull()?.id)
    private var nextId = initialTasks.size

    override val tasks: StateFlow<List<Task>> = taskList.asStateFlow()
    override val currentTask = combine(taskList, currentId) { list, id -> list.firstOrNull { it.id == id } }
    override val todayStats: StateFlow<FocusStats> = MutableStateFlow(stats).asStateFlow()

    override suspend fun addTask(title: String): Task {
        val task = Task(id = "local_${nextId++}", title = title)
        taskList.update { it + task }
        currentId.value = task.id
        return task
    }

    override suspend fun selectTask(id: String) {
        if (taskList.value.any { it.id == id }) currentId.value = id
    }
}
