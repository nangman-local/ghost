package com.ghost.prototype.ui

import androidx.compose.runtime.Composable
import com.ghost.prototype.ui.navigation.GhostNavHost

/** 앱 루트. `GhostTheme` 적용은 온보딩·홈과 함께 한다(#47). */
@Composable
fun GhostApp() {
    GhostNavHost()
}
