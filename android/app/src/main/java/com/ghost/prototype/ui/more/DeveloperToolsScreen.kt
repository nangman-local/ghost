package com.ghost.prototype.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.prototype.GhostApplication
import com.ghost.prototype.MainViewModel
import com.ghost.prototype.R
import com.ghost.prototype.contract.Permission
import com.ghost.prototype.detection.DetectionPanel
import com.ghost.prototype.detection.DetectionState
import com.ghost.prototype.floating.FloatingState
import com.ghost.prototype.ui.openAccessibilitySettings
import com.ghost.prototype.ui.openOverlaySettings
import com.ghost.prototype.ui.openUsageSettings

/**
 * 개발자 도구: 기존 프로토타입 화면(권한·시작/종료·감지 패널) 그대로.
 * 실기기 검증용이라 `ui/` 규칙의 예외로 `floating/`·`detection/`을 직접 참조한다.
 */
@Composable
fun DeveloperToolsRoute(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val permissions = (context.applicationContext as GhostApplication).permissionStatus
    var overlayPermission by remember { mutableStateOf(false) }
    var usagePermission by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayPermission = permissions.isGranted(Permission.OVERLAY)
        usagePermission = permissions.isGranted(Permission.USAGE_ACCESS)
    }
    val floating by viewModel.state.collectAsStateWithLifecycle()
    val detection by viewModel.detection.collectAsStateWithLifecycle()
    val unavailable = viewModel::permissionSettingsUnavailable

    DeveloperToolsScreen(
        floating = floating,
        detection = detection,
        overlayPermission = overlayPermission,
        usagePermission = usagePermission,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        openOverlaySettings = { context.openOverlaySettings(unavailable) },
        openUsageSettings = { context.openUsageSettings(unavailable) },
        openAccessibilitySettings = { context.openAccessibilitySettings(unavailable) },
    )
}

@Composable
fun DeveloperToolsScreen(
    floating: FloatingState,
    detection: DetectionState,
    overlayPermission: Boolean,
    usagePermission: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    openOverlaySettings: () -> Unit,
    openUsageSettings: () -> Unit,
    openAccessibilitySettings: () -> Unit,
) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6354B5))) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("GHOST", style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.prototype_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.prototype_description))
                Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (overlayPermission) R.string.permission_granted else R.string.permission_required))
                OutlinedButton(onClick = openOverlaySettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.permission_button))
                }
                Text(stringResource(if (floating.visible) R.string.status_running else R.string.status_stopped))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onStart,
                        enabled = overlayPermission && !floating.visible,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.start)) }
                    OutlinedButton(
                        onClick = onStop,
                        enabled = floating.visible,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.stop)) }
                }
                floating.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                DetectionPanel(detection, usagePermission, openUsageSettings, openAccessibilitySettings)
                Text(stringResource(R.string.prototype_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
