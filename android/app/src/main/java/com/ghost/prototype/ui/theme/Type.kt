package com.ghost.prototype.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Figma는 Pretendard Variable. 폰트 파일은 #47에서 추가하고 이 값을 교체한다.
internal val GhostFontFamily: FontFamily = FontFamily.Default

// Figma 14.25px(기기 목업 배율)는 14sp로 반올림한다.
internal val GhostTypography = Typography(
    // 온보딩 제목: Bold 20/28, tracking -0.4px
    headlineSmall = TextStyle(
        fontFamily = GhostFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em,
    ),
    // 온보딩 설명: Regular 16/24, tracking -0.32px
    bodyLarge = TextStyle(
        fontFamily = GhostFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.02).em,
    ),
    // 버튼·카드 제목·말풍선
    labelLarge = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    // 기록 라벨
    labelMedium = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Light, fontSize = 12.sp),
    // 진행률 라벨
    bodySmall = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    // 하단 탭 라벨
    labelSmall = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp),
)
