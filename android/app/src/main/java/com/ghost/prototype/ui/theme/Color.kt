package com.ghost.prototype.ui.theme

import androidx.compose.ui.graphics.Color

// Figma "UI" 페이지(node 1:125)에서 추출. Figma 변수가 거의 없어 raw 값 기준이다.
internal val Background = Color(0xFF040309)
internal val Surface = Color(0xFF1D1D25)
internal val SurfaceDim = Color(0xFF16161C)
internal val SurfaceSelected = Color.White.copy(alpha = 0.08f)
internal val InputBackground = Color.Black.copy(alpha = 0.25f)
internal val InputBorder = Color.White.copy(alpha = 0.2f)
internal val OnBackground = Color.White
internal val OnMuted = Color.White.copy(alpha = 0.8f)
// Figma는 18%지만 대비가 약 1.3:1이라 올렸다.
internal val Placeholder = Color.White.copy(alpha = 0.4f)
internal val PrimaryButton = Color.White
internal val OnPrimaryButton = Color.Black
internal val Accent = Color(0xFF5D51C9)
internal val Progress = Color(0xFF9AA0E4)
internal val GhostGlow = Color(0xFF989BE8)
internal val GhostInner = Color(0xFF7281FF)
