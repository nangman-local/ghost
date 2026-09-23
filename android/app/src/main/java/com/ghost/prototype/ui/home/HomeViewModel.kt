package com.ghost.prototype.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ghost.prototype.GhostApplication
import com.ghost.prototype.contract.FocusController
import com.ghost.prototype.contract.TaskRepository
import com.ghost.prototype.ui.home.taskpicker.TaskPickerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** contract(TaskRepository · FocusController)만 보고 홈 상태를 만든다. 의존성은 주입받아 단위 테스트한다. */
class HomeViewModel(
    private val tasks: TaskRepository,
    private val focus: FocusController,
) : ViewModel() {

    private data class LocalState(
        val input: String = "",
        val pickerOpen: Boolean = false,
        val pickerInput: String = "",
        val settingsUnavailable: Boolean = false,
    )

    private val local = MutableStateFlow(LocalState())

    private val data = combine(tasks.tasks, tasks.currentTask, tasks.todayStats, ::Triple)

    val state: StateFlow<HomeUiState> = combine(data, focus.state, local) { (all, current, stats), snapshot, ui ->
        HomeUiState(
            content = homeContent(
                current, snapshot.taskId, snapshot.state, all, stats, ui.input,
                sessionDistractSeconds = snapshot.distractSeconds,
            ),
            error = if (ui.settingsUnavailable) HomeError.SettingsUnavailable else snapshot.error?.let(HomeError::Focus),
            picker = if (ui.pickerOpen) TaskPickerUiState(all, current?.id, ui.pickerInput) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onInputChange(value: String) = local.update { it.copy(input = value) }

    fun addTask() {
        val title = local.value.input.trim()
        if (title.isEmpty()) return
        local.update { it.copy(input = "") }
        viewModelScope.launch { tasks.addTask(title) }
    }

    /** 오류가 나도 자동으로 다시 시도하지 않는다. 사용자가 다시 누른다. */
    fun startTask() {
        viewModelScope.launch {
            val task = tasks.currentTask.first() ?: return@launch
            local.update { it.copy(settingsUnavailable = false) }
            focus.startTask(task.id)
        }
    }

    fun stopTask() = focus.stop()

    fun openPicker() = local.update { it.copy(pickerOpen = true) }

    fun closePicker() = local.update { it.copy(pickerOpen = false, pickerInput = "") }

    fun onPickerInputChange(value: String) = local.update { it.copy(pickerInput = value) }

    fun selectTask(id: String) {
        viewModelScope.launch { tasks.selectTask(id) }
        closePicker()
    }

    fun addTaskFromPicker() {
        val title = local.value.pickerInput.trim()
        if (title.isEmpty()) return
        viewModelScope.launch { tasks.addTask(title) }
        closePicker()
    }

    fun settingsUnavailable() = local.update { it.copy(settingsUnavailable = true) }

    companion object {
        /** 앱에서는 `GhostApplication`의 연결(Fake 또는 실제 구현)을 쓴다. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as GhostApplication
                HomeViewModel(app.taskRepository, app.focusController)
            }
        }
    }
}
