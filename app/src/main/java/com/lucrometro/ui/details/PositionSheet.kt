package com.lucrometro.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.parseDecimal
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * The add/edit position sheet (decisions 0033, 0040): the average price, which the owner copies from the broker. Editing
 * ([previous] not null) starts empty and shows the previous value below the field. The error goes below the field, as
 * in the add-ticker sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionSheet(
    previous: Position?,
    quote: Quote,
    formats: QuoteFormats,
    /** Saves the average price; false if it could not be saved. */
    onSave: suspend (averagePrice: Double) -> Boolean,
    onClose: () -> Unit,
) {
    val average = rememberTextFieldState()
    var averageError by rememberSaveable { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThenClose: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }

    ClearOnEdit(average) { averageError = null }

    val save: () -> Unit = save@{
        // The keyboard's Done still works while the field is read-only: one save at a time.
        if (saving) return@save
        val averageValue = parseDecimal(average.text.toString())?.takeIf { it > 0 }
        if (averageValue == null) {
            averageError = R.string.position_error_average
            return@save
        }
        saving = true
        scope.launch {
            if (onSave(averageValue)) {
                hideThenClose()
            } else {
                averageError = R.string.position_error_save
                saving = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
            Text(stringResource(R.string.position_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (previous == null) R.string.position_sheet_body_add else R.string.position_sheet_body_edit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            NumberField(
                state = average,
                label = quote.currency?.let { stringResource(R.string.position_field_average_currency, it) }
                    ?: stringResource(R.string.position_field_average),
                supporting = previous?.let { stringResource(R.string.position_was, formats.exact(quote, it.averagePrice)) }
                    ?: stringResource(R.string.position_field_average_hint),
                error = averageError,
                readOnly = saving,
                onDone = save,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = hideThenClose) { Text(stringResource(R.string.cancel)) }
                Button(onClick = save, enabled = average.text.isNotBlank() && !saving) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    state: TextFieldState,
    label: String,
    supporting: String,
    error: Int?,
    readOnly: Boolean,
    onDone: () -> Unit,
) {
    OutlinedTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text(label) },
        supportingText = { Text(error?.let { stringResource(it) } ?: supporting) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        onKeyboardAction = { onDone() },
    )
}

/** Calls [onEdit] on every real edit of [state] (not its initial value), so an error goes away once the owner types. */
@Composable
private fun ClearOnEdit(state: TextFieldState, onEdit: () -> Unit) {
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.drop(1).collect { onEdit() } }
}
