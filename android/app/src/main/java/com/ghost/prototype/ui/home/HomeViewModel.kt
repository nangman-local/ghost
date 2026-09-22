package com.ghost.prototype.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.prototype.GhostApplication
import com.ghost.prototype.R
import com.ghost.prototype.contract.Permission
import com.ghost.prototype.ui.home.taskpicker.TaskPickerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** contract(TaskRepository · FocusController · PermissionStatus)만 보고 홈 상태를 만든다. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GhostApplication
    private val tasks = app.taskRepository
    private val focus = app.focusController

    private data class LocalState(
        val input: String = "",
        val pickerOpen: Boolean = false,
        val pickerInput: String = "",
        val overlayGranted: Boolean = true,
        val error: String? = null,
    )

    private val local = MutableStateFlow(LocalState())

    private val data = combine(tasks.tasks, tasks.currentTask, tasks.todayStats, ::Triple)

    val state: StateFlow<HomeUiState> = combine(data, focus.state, local) { (all, current, stats), snapshot, ui ->
        HomeUiState(
            content = homeContent(current, snapshot.taskId, snapshot.state, all, stats, ui.input),
            error = ui.error ?: snapshot.error,
            overlayMissing = !ui.overlayGranted && (ui.error ?: snapshot.error) != null,
            picker = if (ui.pickerOpen) TaskPickerUiState(all, current?.id, ui.pickerInput) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** onResume마다 호출. 권한이 돌아와도 자동으로 다시 시작하지 않는다. */
    fun refreshPermissions() {
        local.update { it.copy(overlayGranted = app.permissionStatus.isGranted(Permission.OVERLAY)) }
    }

    fun onInputChange(value: String) = local.update { it.copy(input = value) }

    fun addTask() {
        val title = local.value.input.trim()
        if (title.isEmpty()) return
        local.update { it.copy(input = "") }
        viewModelScope.launch { tasks.addTask(title) }
    }

    fun startTask() {
        viewModelScope.launch {
            val task = tasks.currentTask.first() ?: return@launch
            local.update { it.copy(error = null) }
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

    fun settingsUnavailable() {
        local.update { it.copy(error = app.getString(R.string.error_settings)) }
    }
}
