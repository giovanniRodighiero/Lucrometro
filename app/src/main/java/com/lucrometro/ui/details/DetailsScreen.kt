package com.lucrometro.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.R
import com.lucrometro.ui.watchlist.rememberQuoteFormats

/**
 * Details of one ticker (decision 0023). Iteration 2: top bar and quote header only; the market
 * status, chart, readout and range bar come next.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    code: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = viewModel(key = code, factory = DetailsViewModel.factory(code)),
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(code) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            entry?.let { QuoteHeader(it, rememberQuoteFormats()) }
        }
    }
}
