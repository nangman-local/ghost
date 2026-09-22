package com.ghost.prototype.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Material 색 체계에 없는 Figma 토큰. `GhostTheme.colors`로 읽는다. */
@Immutable
data class GhostColors(
    val surfaceDim: Color = SurfaceDim,
    val surfaceSelected: Color = SurfaceSelected,
    val inputBackground: Color = InputBackground,
    val inputBorder: Color = InputBorder,
    val onMuted: Color = OnMuted,
    val placeholder: Color = Placeholder,
    val primaryButton: Color = PrimaryButton,
    val onPrimaryButton: Color = OnPrimaryButton,
    val accent: Color = Accent,
    val progress: Color = Progress,
    val ghostGlow: Color = GhostGlow,
    val ghostInner: Color = GhostInner,
)

private val LocalGhostColors = staticCompositionLocalOf { GhostColors() }

private val GhostColorScheme = darkColorScheme(
    primary = PrimaryButton,
    onPrimary = OnPrimaryButton,
    secondary = Accent,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnBackground,
    surfaceVariant = Surface,
    onSurfaceVariant = OnMuted,
    outline = InputBorder,
)

private val GhostShapes = Shapes(
    small = RoundedCornerShape(8.dp), // 입력칸·아이콘 박스
    large = RoundedCornerShape(20.dp), // 카드
    extraLarge = RoundedCornerShape(100.dp), // 버튼·말풍선·하단 탭
)

/** 다크 전용 테마. Figma에 라이트 시안이 없다. */
@Composable
fun GhostTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGhostColors provides GhostColors()) {
        MaterialTheme(
            colorScheme = GhostColorScheme,
            typography = GhostTypography,
            shapes = GhostShapes,
            content = content,
        )
    }
}

object GhostTheme {
    val colors: GhostColors
        @Composable get() = LocalGhostColors.current
}
