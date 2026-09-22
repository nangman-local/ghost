package com.ghost.prototype.ui.home.taskpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R
import com.ghost.prototype.contract.Task
import com.ghost.prototype.ui.component.PrimaryButton
import com.ghost.prototype.ui.component.TaskInputField
import com.ghost.prototype.ui.theme.GhostTheme

/**
 * 할 일 목록·추가 팝업. **디자인 시안이 없어 기존 토큰으로 임시 구성했다.**
 * 시안이 나오면 [TaskPickerContent]만 교체한다. 띄우는 방식(시트/전체 화면)은 [TaskPickerSheet]가 맡는다.
 */
data class TaskPickerUiState(
    val tasks: List<Task> = emptyList(),
    val selectedId: String? = null,
    val input: String = "",
)

class TaskPickerActions(
    val onSelect: (String) -> Unit,
    val onInputChange: (String) -> Unit,
    val onAdd: () -> Unit,
    val onDismiss: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPickerSheet(state: TaskPickerUiState, actions: TaskPickerActions) {
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        TaskPickerContent(state, actions, Modifier.navigationBarsPadding())
    }
}

@Composable
fun TaskPickerContent(state: TaskPickerUiState, actions: TaskPickerActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.task_picker_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (state.tasks.isEmpty()) {
            Text(
                stringResource(R.string.task_picker_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = GhostTheme.colors.onMuted,
            )
        } else {
            LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.tasks, key = { it.id }) { task ->
                    val selected = task.id == state.selectedId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (selected) GhostTheme.colors.surfaceSelected else Color.Transparent)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { actions.onSelect(task.id) })
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = GhostTheme.colors.progress)
                        }
                    }
                }
            }
        }
        TaskInputField(
            value = state.input,
            onValueChange = actions.onInputChange,
            placeholder = stringResource(R.string.home_task_placeholder),
            onSubmit = actions.onAdd,
        )
        PrimaryButton(stringResource(R.string.home_add_task), onClick = actions.onAdd)
    }
}

@Preview
@Composable
private fun TaskPickerContentPreview() {
    GhostTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surface).clickable {}) {
            TaskPickerContent(
                TaskPickerUiState(listOf(Task("1", "기말 과제 마감"), Task("2", "자료구조 복습")), selectedId = "1"),
                TaskPickerActions({}, {}, {}, {}),
            )
        }
    }
}
