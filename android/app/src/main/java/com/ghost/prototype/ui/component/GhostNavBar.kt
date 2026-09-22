package com.ghost.prototype.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ghost.prototype.R
import com.ghost.prototype.ui.theme.GhostTheme

enum class MainTab(@DrawableRes val icon: Int, @StringRes val label: Int) {
    CALENDAR(R.drawable.ic_nav_calendar, R.string.nav_calendar),
    HOME(R.drawable.ic_nav_home, R.string.nav_home),
    MORE(R.drawable.ic_nav_more, R.string.nav_more),
}

/** 하단 탭(Figma Navbar 49:2076). 폭은 화면에 맞춰 늘어난다. */
@Composable
fun GhostNavBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(if (isSelected) GhostTheme.colors.surfaceSelected else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                Icon(painterResource(tab.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Preview
@Composable
private fun GhostNavBarPreview() {
    GhostTheme { GhostNavBar(MainTab.HOME, {}) }
}
