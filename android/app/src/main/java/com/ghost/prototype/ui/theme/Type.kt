package com.ghost.prototype.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ghost.prototype.R

// Pretendard 1.3.9 (SIL OFL, android/third_party/pretendard/LICENSE.txt). Figma에서 쓰는 4종만 넣는다.
internal val GhostFontFamily = FontFamily(
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

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
    // Figma에 없는 기본 본문·소제목 (Text 기본 스타일이 bodyLarge라 따로 쓸 때만)
    bodyMedium = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    titleMedium = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    // 버튼·카드 제목·말풍선
    labelLarge = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    // 기록 라벨
    labelMedium = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Light, fontSize = 12.sp),
    // 진행률 라벨
    bodySmall = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    // 하단 탭 라벨
    labelSmall = TextStyle(fontFamily = GhostFontFamily, fontWeight = FontWeight.Normal, fontSize = 10.sp),
)
