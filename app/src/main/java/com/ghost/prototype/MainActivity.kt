package com.ghost.prototype

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.prototype.detection.DetectionPanel
import com.ghost.prototype.detection.DetectionPermissions

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var overlayPermission by mutableStateOf(false)
    private var usagePermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val floating by viewModel.state.collectAsStateWithLifecycle()
            val detection by viewModel.detection.collectAsStateWithLifecycle()
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
                        OutlinedButton(onClick = ::openOverlaySettings, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.permission_button))
                        }
                        Text(stringResource(if (floating.visible) R.string.status_running else R.string.status_stopped))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = viewModel::start,
                                enabled = overlayPermission && !floating.visible,
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.start)) }
                            OutlinedButton(
                                onClick = viewModel::stop,
                                enabled = floating.visible,
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.stop)) }
                        }
                        floating.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        DetectionPanel(detection, usagePermission, ::openUsageSettings, ::openAccessibilitySettings)
                        Text(stringResource(R.string.prototype_note), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayPermission = Settings.canDrawOverlays(this)
        usagePermission = DetectionPermissions.usageAccess(this)
    }

    private fun openUsageSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            viewModel.permissionSettingsUnavailable()
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            viewModel.permissionSettingsUnavailable()
        }
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (_: ActivityNotFoundException) {
            viewModel.permissionSettingsUnavailable()
        }
    }
}
