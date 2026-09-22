package com.ghost.prototype.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R
import com.ghost.prototype.ui.theme.GhostTheme

/** 캘린더 탭 자리. 디자인이 나오면 채운다(캘린더 연동은 #21). */
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.calendar_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = GhostTheme.colors.onMuted,
            textAlign = TextAlign.Center,
        )
    }
}
