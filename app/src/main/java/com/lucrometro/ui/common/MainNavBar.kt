package com.lucrometro.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lucrometro.R

/** The two top-level destinations (decision 0049). */
enum class MainTab { Watchlist, Portfolio }

/** Doop component NavBar (decision 0051): Material 3 navigation bar, shown under both tabs, never on pushed screens. */
@Composable
fun MainNavBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == MainTab.Watchlist,
            onClick = { onSelect(MainTab.Watchlist) },
            icon = { Icon(painterResource(R.drawable.ic_show_chart), contentDescription = null) },
            label = { Text(stringResource(R.string.nav_watchlist)) },
        )
        NavigationBarItem(
            selected = selected == MainTab.Portfolio,
            onClick = { onSelect(MainTab.Portfolio) },
            icon = {
                val icon = if (selected == MainTab.Portfolio) R.drawable.ic_savings_filled else R.drawable.ic_savings
                Icon(painterResource(icon), contentDescription = null)
            },
            label = { Text(stringResource(R.string.nav_portfolio)) },
        )
    }
}
