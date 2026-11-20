package com.lucrometro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.ui.details.DetailsScreen
import com.lucrometro.ui.theme.LucrometroTheme
import com.lucrometro.ui.watchlist.WatchlistScreen
import com.lucrometro.ui.watchlist.WatchlistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LucrometroTheme {
                // Two screens: the open ticker's code is the whole navigation state (decision 0024).
                var openCode by rememberSaveable { mutableStateOf<String?>(null) }
                BackHandler(enabled = openCode != null) { openCode = null }
                // PRD: auto-refresh when the app is opened or brought back (skipped if it refreshed a moment ago). Above
                // the switch: coming back from details is not opening the app, and must not refetch the whole list.
                val watchlist: WatchlistViewModel = viewModel(factory = WatchlistViewModel.Factory)
                LifecycleStartEffect(watchlist) {
                    watchlist.onAppStarted()
                    onStopOrDispose {}
                }
                // Above the switch, so the list keeps its scroll position while details is open.
                val listState = rememberLazyListState()
                val code = openCode
                if (code == null) {
                    WatchlistScreen(onOpenTicker = { openCode = it }, listState = listState, viewModel = watchlist)
                } else {
                    DetailsScreen(code, onBack = { openCode = null })
                }
            }
        }
    }
}
