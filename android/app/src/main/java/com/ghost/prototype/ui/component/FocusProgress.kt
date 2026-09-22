package com.ghost.prototype.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R
import com.ghost.prototype.ui.theme.GhostTheme

/** 진행바 위 체크포인트. 의미(시간 기준인지 작업 단계인지)는 아직 정해지지 않아 비율과 라벨만 받는다. */
data class Checkpoint(val fraction: Float, val label: String, val done: Boolean)

private val MarkerWidth = 34.dp

/**
 * 진행바(Figma 56:626). **왼쪽 끝에 붙고 오른쪽은 44dp 비운다**(의도된 비대칭).
 * 채움 폭과 체크포인트 위치는 비율로 계산해 화면 폭이 달라도 맞는다.
 */
@Composable
fun FocusProgress(progress: Float, checkpoints: List<Checkpoint>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(end = 44.dp)) {
        val width = maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(Modifier.fillMaxWidth().height(13.dp)) {
                Box(Modifier.fillMaxWidth().height(13.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)))
                Box(
                    Modifier
                        .padding(vertical = 0.5.dp)
                        .width(width * progress.coerceIn(0f, 1f))
                        .height(12.dp)
                        .background(GhostTheme.colors.progress, RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)),
                )
                checkpoints.forEach {
                    Box(
                        Modifier
                            .offset(x = width * it.fraction - 3.dp, y = 3.5.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape),
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                checkpoints.forEach {
                    Column(
                        Modifier.offset(x = width * it.fraction - MarkerWidth / 2).width(MarkerWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            it.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        Box(
                            Modifier.size(26.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = if (it.done) GhostTheme.colors.progress else GhostTheme.colors.onMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
