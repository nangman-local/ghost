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
fun DetectionPanel(state: DetectionState, usagePermission: Boolean, openUsageSettings: () -> Unit) {
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
    }
}

private fun statusText(status: ObservationStatus): Int = when (status) {
    ObservationStatus.STOPPED -> R.string.detection_stopped
    ObservationStatus.PERMISSION_REQUIRED -> R.string.detection_permission_required
    ObservationStatus.OBSERVING -> R.string.detection_observing
    ObservationStatus.UNAVAILABLE -> R.string.detection_unavailable
}
