package com.lucrometro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.lucrometro.ui.common.MainNavBar
import com.lucrometro.ui.common.MainTab
import com.lucrometro.ui.details.DetailsScreen
import com.lucrometro.ui.portfolio.PortfolioScreen
import com.lucrometro.ui.portfolio.PortfolioViewModel
import com.lucrometro.ui.theme.LucrometroTheme
import com.lucrometro.ui.trades.TradesScreen
import com.lucrometro.ui.watchlist.WatchlistScreen
import com.lucrometro.ui.watchlist.WatchlistViewModel
import com.lucrometro.ui.yeardetail.YearDetailScreen

/**
 * A place in the app: the back stack is a list of these (Navigation 3). [Home] is the two tabs under the navigation bar
 * (decision 0051); the others are pushed over it, without the bar.
 */
sealed interface Screen {
    data object Home : Screen
    data class Details(val code: String) : Screen
    data class Trades(val code: String) : Screen
    data class Year(val year: Int) : Screen
}

// Saved as plain strings, so the keys need no serialization plugin: "W", "D:ENI.MI", "T:ENI.MI", "Y:2025".
private val BackStackSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { stack ->
        stack.map {
            when (it) {
                Screen.Home -> "W"
                is Screen.Details -> "D:${it.code}"
                is Screen.Trades -> "T:${it.code}"
                is Screen.Year -> "Y:${it.year}"
            }
        }
    },
    restore = { saved ->
        saved.map {
            when (it.substringBefore(':')) {
                "D" -> Screen.Details(it.substringAfter(':'))
                "T" -> Screen.Trades(it.substringAfter(':'))
                "Y" -> it.substringAfter(':').toIntOrNull()?.let(Screen::Year) ?: Screen.Home
                else -> Screen.Home
            }
        }.toMutableStateList()
    },
)

private fun List<Screen>.toMutableStateList() = mutableStateListOf<Screen>().also { it.addAll(this) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LucrometroTheme {
                val backStack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Screen>(Screen.Home) }
                var tab by rememberSaveable { mutableStateOf(MainTab.Watchlist) }
                // Public or private view (decision 0042): one tap on the wallet, starts public on every launch.
                var private by rememberSaveable { mutableStateOf(false) }
                // PRD: auto-refresh when the app is opened or brought back (skipped if it refreshed a moment ago). Above
                // the screens: coming back from details is not opening the app, and must not refetch the whole list.
                val watchlist: WatchlistViewModel = viewModel(factory = WatchlistViewModel.Factory)
                // Above the tabs, so a year-end fetch goes on while the watchlist is shown.
                val portfolio: PortfolioViewModel = viewModel(factory = PortfolioViewModel.Factory)
                LifecycleStartEffect(watchlist) {
                    watchlist.onAppStarted()
                    portfolio.onAppStarted()
                    onStopOrDispose {}
                }
                // Back on the Portfolio tab goes to the Watchlist first, as with any navigation bar.
                BackHandler(enabled = tab == MainTab.Portfolio && backStack.size == 1) { tab = MainTab.Watchlist }
                // Above the screens, so the list keeps its scroll position while details is open.
                val listState = rememberLazyListState()
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { screen ->
                        NavEntry(screen) {
                            when (screen) {
                                // One navigation bar under both tabs, so it stays still while the tab above it changes.
                                Screen.Home -> Column {
                                    AnimatedContent(
                                        tab,
                                        // The bar below takes the system navigation inset: the tabs' Scaffolds must not add it again.
                                        Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars),
                                        // Material fade-through between the tabs: out quickly, then in with a slight zoom.
                                        transitionSpec = {
                                            (fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f))
                                                .togetherWith(fadeOut(tween(90)))
                                        },
                                        label = "tabs",
                                    ) { shown ->
                                        when (shown) {
                                            MainTab.Watchlist -> WatchlistScreen(
                                                onOpenTicker = { backStack.add(Screen.Details(it)) },
                                                private = private,
                                                onPrivateChange = { private = it },
                                                listState = listState,
                                                viewModel = watchlist,
                                            )
                                            MainTab.Portfolio -> PortfolioScreen(
                                                viewModel = portfolio,
                                                private = private,
                                                onPrivateChange = { private = it },
                                                onOpenTicker = { backStack.add(Screen.Details(it)) },
                                                onOpenYear = { backStack.add(Screen.Year(it)) },
                                            )
                                        }
                                    }
                                    MainNavBar(tab) { tab = it }
                                }
                                is Screen.Details -> DetailsScreen(
                                    screen.code,
                                    private = private,
                                    onPrivateChange = { private = it },
                                    onBack = { backStack.removeLastOrNull() },
                                    onOpenTrades = { backStack.add(Screen.Trades(screen.code)) },
                                )
                                is Screen.Trades -> TradesScreen(screen.code, onBack = { backStack.removeLastOrNull() })
                                is Screen.Year -> {
                                    // Restored straight onto a year (process death): the tab never ran, so load here too.
                                    LaunchedEffect(portfolio) { portfolio.onShown() }
                                    YearDetailScreen(screen.year, onBack = { backStack.removeLastOrNull() })
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
