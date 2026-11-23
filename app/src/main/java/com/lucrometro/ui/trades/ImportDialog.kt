package com.lucrometro.ui.trades

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.CsvError
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.data.Side
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** A trade CSV is a few kilobytes: anything past this is not one, and is refused instead of filling the memory. */
private const val MAX_FILE_BYTES = 2 * 1024 * 1024

/** The picked file's name and its text (UTF-8); a null text when it can't be read or is too big. */
internal suspend fun readPickedFile(context: Context, uri: Uri): Pair<String?, String?> = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val name = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()
    val text = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (out.size() + n > MAX_FILE_BYTES) return@use null
                out.write(buffer, 0, n)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()
    name to text
}

/**
 * The import dialog (Doop Dialog, decision 0042): before saving, what the file adds (buys and sells, dates, what was
 * skipped, shares and average after); or why nothing was saved.
 */
@Composable
fun ImportDialog(
    state: ImportState,
    code: String,
    typed: Position?,
    hasTrades: Boolean,
    quote: Quote,
    formats: QuoteFormats,
    times: TimeFormats,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is ImportState.Confirm -> {
            val check = state.check
            val buys = check.new.count { it.side == Side.BUY }
            val sells = check.new.size - buys
            val dates = check.new.map { it.date }
            val first = times.date("dMMMyyyy", dates.min())
            val last = times.date("dMMMyyyy", dates.max())
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(pluralStringResource(R.plurals.import_title, check.new.size, check.new.size)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.import_from, state.fileName ?: "CSV", code), Modifier.padding(bottom = 8.dp))
                        Line(
                            stringResource(R.string.import_new),
                            listOfNotNull(
                                buys.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.import_buys, it, it) },
                                sells.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.import_sells, it, it) },
                            ).joinToString(", "),
                        )
                        Line(stringResource(R.string.import_dates), if (first == last) first else "$first – $last")
                        val skipped = listOfNotNull(
                            check.otherTickers.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.import_skipped_other, it, it) },
                            check.alreadySaved.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.import_skipped_saved, it, it) },
                        )
                        if (skipped.isNotEmpty()) Line(stringResource(R.string.import_skipped), skipped.joinToString(", "))
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        val average = check.after.average
                        Line(
                            stringResource(R.string.import_after),
                            if (average == null) {
                                stringResource(R.string.import_after_sold_out)
                            } else {
                                stringResource(R.string.import_after_value, formats.shares(check.after.shares), formats.price(quote, average))
                            },
                        )
                        // The log wins (decision 0040): say so the first time trades replace a typed average.
                        if (typed != null && !hasTrades) {
                            Text(
                                stringResource(R.string.import_replaces, withCurrency(quote, formats.exact(quote, typed.averagePrice))),
                                Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_action)) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }
        is ImportState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_error_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(problemText(state.problem, code, times))
                    Text(stringResource(R.string.import_error_nothing_saved))
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        )
    }
}

/** A label and its value on one line, the value at the end. */
@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label)
        Text(value, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}

@Composable
private fun problemText(problem: ImportProblem, code: String, times: TimeFormats): String = when (problem) {
    ImportProblem.Unreadable -> stringResource(R.string.import_error_read)
    ImportProblem.NoRows -> stringResource(R.string.import_error_no_rows, code)
    ImportProblem.SaveFailed -> stringResource(R.string.import_save_failed)
    is ImportProblem.BelowZero -> stringResource(R.string.import_error_below_zero, times.date("dMMMyyyy", problem.trade.date))
    is ImportProblem.Future -> stringResource(R.string.import_error_future, times.date("dMMMyyyy", problem.trade.date))
    is ImportProblem.Csv -> when (val e = problem.error) {
        is CsvError.Missing -> stringResource(R.string.import_error_missing, e.column)
        is CsvError.Columns -> stringResource(R.string.import_error_columns, e.line)
        is CsvError.BadSide -> stringResource(R.string.import_error_side, e.line, e.value)
        is CsvError.BadDate -> stringResource(R.string.import_error_date, e.line, e.value)
        is CsvError.BadTime -> stringResource(R.string.import_error_time, e.line, e.value)
        is CsvError.NotANumber -> stringResource(R.string.import_error_number, e.line, e.column, e.value)
        is CsvError.NotPositive -> stringResource(R.string.import_error_positive, e.line, e.column)
    }
}
