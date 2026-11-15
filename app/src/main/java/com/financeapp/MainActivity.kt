package com.financeapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financeapp.data.CronetHttp
import com.financeapp.data.QuoteSource
import com.financeapp.data.SPIKE_TICKERS
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
                QuoteScreen(runAll = { onLine, onDone ->
                    worker.execute {
                        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.US)
                        val t0 = System.currentTimeMillis()
                        var ok = 0
                        for (sym in SPIKE_TICKERS) {
                            val s = System.currentTimeMillis()
                            val r = source.quote(sym)
                            val ms = System.currentTimeMillis() - s
                            val line = r.fold(
                                onSuccess = {
                                    ok++
                                    "OK   %-8s %9.3f %s  %s  %dms".format(
                                        sym, it.price, it.currency ?: "", fmt.format(Date(it.marketTimeEpoch * 1000)), ms,
                                    )
                                },
                                onFailure = { "FAIL %-8s %s  %dms".format(sym, it.message?.take(70), ms) },
                            )
                            Log.i("QuoteTest", line)
                            runOnUiThread { onLine(line) }
                        }
                        val total = System.currentTimeMillis() - t0
                        val summary = "DONE $ok/${SPIKE_TICKERS.size} ok in ${total}ms"
                        Log.i("QuoteTest", summary)
                        runOnUiThread { onDone(summary) }
                    }
                })
            }
        }
    }
}

@Composable
fun QuoteScreen(runAll: (onLine: (String) -> Unit, onDone: (String) -> Unit) -> Unit) {
    val lines = remember { mutableStateListOf<String>() }
    var summary by remember { mutableStateOf("Running...") }
    var runs by remember { mutableIntStateOf(0) }
    LaunchedEffect(runs) {
        lines.clear()
        summary = "Running..."
        runAll({ lines += it }, { summary = it })
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = { runs++ }) { Text("Run again") }
        Text(summary, Modifier.padding(vertical = 8.dp))
        LazyColumn {
            items(lines) { Text(it, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
        }
    }
}
