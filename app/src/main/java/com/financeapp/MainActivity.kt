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
import com.financeapp.data.HistoryRange
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
                QuoteScreen(runAll = { onLine, onDone ->
                    worker.execute {
                        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val sym = "SMH.MI"
                        for (range in HistoryRange.entries) {
                            val s = System.currentTimeMillis()
                            val r = source.history(sym, range)
                            val ms = System.currentTimeMillis() - s
                            r.fold(
                                onSuccess = { c ->
                                    val head = "%s %s: %d bars  %s -> %s  %dms".format(
                                        sym, range.yahoo, c.size,
                                        day.format(Date(c.first().epochSeconds * 1000)),
                                        day.format(Date(c.last().epochSeconds * 1000)), ms,
                                    )
                                    Log.i("QuoteTest", head)
                                    runOnUiThread { onLine(head) }
                                    c.forEach {
                                        val l = "  %s  o=%s h=%s l=%s c=%.2f v=%s".format(
                                            day.format(Date(it.epochSeconds * 1000)),
                                            it.open?.let { v -> "%.2f".format(v) }, it.high?.let { v -> "%.2f".format(v) },
                                            it.low?.let { v -> "%.2f".format(v) }, it.close, it.volume,
                                        )
                                        Log.i("QuoteTest", l)
                                        runOnUiThread { onLine(l) }
                                    }
                                },
                                onFailure = {
                                    val l = "FAIL $sym ${range.yahoo}: ${it.message?.take(80)}"
                                    Log.i("QuoteTest", l)
                                    runOnUiThread { onLine(l) }
                                },
                            )
                        }
                        Log.i("QuoteTest", "DONE")
                        runOnUiThread { onDone("DONE") }
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
