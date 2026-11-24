package com.lucrometro.ui.common

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lucrometro.R
import com.lucrometro.data.CsvError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** CSV files come with different types depending on the app that saved them: what the file picker offers. */
val CSV_TYPES = arrayOf("text/*", "application/csv", "application/vnd.ms-excel")

/** A trade CSV is a few kilobytes: anything past this is not one, and is refused instead of filling the memory. */
private const val MAX_FILE_BYTES = 2 * 1024 * 1024

/** The picked file's name and its text (UTF-8); a null text when it can't be read or is too big. */
suspend fun readPickedFile(context: Context, uri: Uri): Pair<String?, String?> = withContext(Dispatchers.IO) {
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

/** Why a trade CSV was refused (decision 0041), with the line number. */
@Composable
fun csvErrorText(error: CsvError): String = when (error) {
    is CsvError.Missing -> stringResource(R.string.import_error_missing, error.column)
    is CsvError.Columns -> stringResource(R.string.import_error_columns, error.line)
    is CsvError.BadSide -> stringResource(R.string.import_error_side, error.line, error.value)
    is CsvError.BadDate -> stringResource(R.string.import_error_date, error.line, error.value)
    is CsvError.BadTime -> stringResource(R.string.import_error_time, error.line, error.value)
    is CsvError.NotANumber -> stringResource(R.string.import_error_number, error.line, error.column, error.value)
    is CsvError.NotPositive -> stringResource(R.string.import_error_positive, error.line, error.column)
}
