package com.ghost.prototype.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R

/**
 * 화면용 캐릭터 포즈. Figma에서 글로우까지 포함해 3x PNG로 내보냈다(배경색 #040309 포함).
 * [widthDp]·[heightDp]는 Figma 기준 표시 크기이며, 공간이 부족하면 비율을 유지한 채 줄어든다.
 */
enum class GhostPose(@DrawableRes val drawable: Int, val widthDp: Dp, val heightDp: Dp) {
    DEFAULT(R.drawable.ghost_default, 197.dp, 218.dp),
    CURIOUS(R.drawable.ghost_curious, 232.dp, 252.dp),
    FOCUS(R.drawable.ghost_focus, 240.dp, 219.dp),
    ALERT(R.drawable.ghost_alert, 203.dp, 271.dp),
    PEEK(R.drawable.ghost_peek, 219.dp, 257.dp),
}

/**
 * 캐릭터 그림. Rive(`.riv`)가 나오면 이 Composable 안만 교체한다.
 * 장식용이라 접근성 설명은 두지 않는다(화면의 제목·말풍선이 의미를 전달한다).
 */
@Composable
fun GhostIllustration(pose: GhostPose, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(pose.drawable),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .sizeIn(maxWidth = pose.widthDp, maxHeight = pose.heightDp)
            .aspectRatio(pose.widthDp / pose.heightDp),
    )
}
