package com.financeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.financeapp.data.CronetHttp
import com.financeapp.data.QuoteSource
import com.financeapp.data.YahooQuoteSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val source: QuoteSource by lazy { YahooQuoteSource(CronetHttp(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                QuoteScreen(fetch = { sym, done ->
                    worker.execute {
                        val t0 = System.currentTimeMillis()
                        val r = source.quote(sym)
                        val ms = System.currentTimeMillis() - t0
                        val text = r.fold(
                            onSuccess = {
                                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                    .format(Date(it.marketTimeEpoch * 1000))
                                "${it.symbol}  ${it.price} ${it.currency ?: ""}\n" +
                                    "exchange: ${it.exchange}\nquote time: $ts\n($ms ms)"
                            },
                            onFailure = { "ERROR: ${it::class.simpleName}: ${it.message}\n($ms ms)" },
                        )
                        runOnUiThread { done(text) }
                    }
                })
            }
        }
    }
}

@Composable
fun QuoteScreen(fetch: (String, (String) -> Unit) -> Unit) {
    var text by remember { mutableStateOf("Loading...") }
    var runs by remember { mutableIntStateOf(0) }
    LaunchedEffect(runs) {
        text = "Loading..."
        fetch("SMH.MI") { text = it }
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text)
        Button(onClick = { runs++ }, modifier = Modifier.padding(top = 24.dp)) { Text("Refetch SMH.MI") }
    }
}
