package com.ghost.prototype.ui

import androidx.compose.runtime.Composable
import com.ghost.prototype.ui.navigation.GhostNavHost
import com.ghost.prototype.ui.theme.GhostTheme

@Composable
fun GhostApp() {
    GhostTheme { GhostNavHost() }
}
