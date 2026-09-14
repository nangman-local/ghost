package com.ghost.prototype.detection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R
import java.text.DateFormat
import java.util.Date

@Composable
fun DetectionPanel(
    state: DetectionState,
    usagePermission: Boolean,
    openUsageSettings: () -> Unit,
    openAccessibilitySettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.detection_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.detection_disclosure), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(if (usagePermission) R.string.usage_granted else R.string.usage_required))
        OutlinedButton(onClick = openUsageSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.usage_settings))
        }
        Text(stringResource(R.string.usage_status, stringResource(statusText(state.usageStatus))))
        Text(stringResource(R.string.recent_app, state.recentApp?.let {
            "${it.label}\n${it.packageName}\n${DateFormat.getTimeInstance().format(Date(it.observedAt))}"
        } ?: stringResource(R.string.not_observed)))
        Text(stringResource(R.string.chrome_disclosure), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(if (state.chromeConnected) R.string.chrome_connected else R.string.chrome_disconnected))
        OutlinedButton(onClick = openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.chrome_settings))
        }
        val chromeStatus = when {
            !state.running -> ObservationStatus.STOPPED
            !state.chromeConnected -> ObservationStatus.PERMISSION_REQUIRED
            state.chromeUnavailable -> ObservationStatus.UNAVAILABLE
            else -> ObservationStatus.OBSERVING
        }
        Text(stringResource(R.string.chrome_status, stringResource(statusText(chromeStatus))))
        Text(stringResource(R.string.recent_domain, state.recentChrome?.domain ?: stringResource(R.string.not_observed)))
        Text(stringResource(
            if (state.recentChrome?.titleSource == TitleSource.WINDOW) R.string.recent_window_title else R.string.recent_page_title,
            state.recentChrome?.title ?: stringResource(R.string.not_observed),
        ))
        state.recentChrome?.let {
            Text(stringResource(R.string.chrome_observed_at, DateFormat.getTimeInstance().format(Date(it.observedAt))))
        }
    }
}

private fun statusText(status: ObservationStatus): Int = when (status) {
    ObservationStatus.STOPPED -> R.string.detection_stopped
    ObservationStatus.PERMISSION_REQUIRED -> R.string.detection_permission_required
    ObservationStatus.OBSERVING -> R.string.detection_observing
    ObservationStatus.UNAVAILABLE -> R.string.detection_unavailable
}
