package com.lucrometro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.lucrometro.ui.details.DetailsScreen
import com.lucrometro.ui.theme.LucrometroTheme
import com.lucrometro.ui.watchlist.WatchlistScreen
import com.lucrometro.ui.watchlist.WatchlistViewModel

/** A place in the app: the back stack is a list of these (Navigation 3). */
sealed interface Screen {
    data object Watchlist : Screen
    data class Details(val code: String) : Screen
}

// Saved as plain strings, so the keys need no serialization plugin: "W", "D:ENI.MI".
private val BackStackSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { stack ->
        stack.map {
            when (it) {
                Screen.Watchlist -> "W"
                is Screen.Details -> "D:${it.code}"
            }
        }
    },
    restore = { saved ->
        saved.map {
            when (it.substringBefore(':')) {
                "D" -> Screen.Details(it.substringAfter(':'))
                else -> Screen.Watchlist
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
                val backStack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Screen>(Screen.Watchlist) }
                // PRD: auto-refresh when the app is opened or brought back (skipped if it refreshed a moment ago). Above
                // the screens: coming back from details is not opening the app, and must not refetch the whole list.
                val watchlist: WatchlistViewModel = viewModel(factory = WatchlistViewModel.Factory)
                LifecycleStartEffect(watchlist) {
                    watchlist.onAppStarted()
                    onStopOrDispose {}
                }
                // Above the screens, so the list keeps its scroll position while details is open.
                val listState = rememberLazyListState()
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { screen ->
                        NavEntry(screen) {
                            when (screen) {
                                Screen.Watchlist -> WatchlistScreen(
                                    onOpenTicker = { backStack.add(Screen.Details(it)) },
                                    listState = listState,
                                    viewModel = watchlist,
                                )
                                is Screen.Details -> DetailsScreen(screen.code, onBack = { backStack.removeLastOrNull() })
                            }
                        }
                    },
                )
            }
        }
    }
}
