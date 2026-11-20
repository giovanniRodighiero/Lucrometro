package com.lucrometro.ui.details

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DAY_MILLIS = 86_400_000L

/**
 * The add/edit position sheet (decision 0033): average price, shares (optional) and the date of the buy, which the
 * owner copies from the broker. Editing ([previous] not null) starts empty and shows the previous values below the
 * fields. Errors go below their field, as in the add-ticker sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionSheet(
    previous: Position?,
    quote: Quote,
    formats: QuoteFormats,
    times: TimeFormats,
    /** Saves the entry; false if it could not be saved. */
    onSave: suspend (averagePrice: Double, shares: Double?, lastBuy: LocalDate) -> Boolean,
    onClose: () -> Unit,
) {
    val average = rememberTextFieldState()
    val shares = rememberTextFieldState()
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var averageError by rememberSaveable { mutableStateOf<Int?>(null) }
    var sharesError by rememberSaveable { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThenClose: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }

    ClearOnEdit(average) { averageError = null }
    ClearOnEdit(shares) { sharesError = null }

    val save: () -> Unit = save@{
        val averageValue = parseDecimal(average.text.toString())?.takeIf { it > 0 }
        val sharesText = shares.text.toString()
        val sharesValue = parseDecimal(sharesText)?.takeIf { it > 0 }
        averageError = if (averageValue == null) R.string.position_error_average else null
        sharesError = if (sharesText.isNotBlank() && sharesValue == null) R.string.position_error_shares else null
        if (averageValue == null || averageError != null || sharesError != null) return@save
        saving = true
        scope.launch {
            if (onSave(averageValue, sharesValue, date)) {
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
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(16.dp))
            NumberField(
                state = shares,
                label = stringResource(R.string.position_field_shares),
                supporting = previous?.shares?.let { stringResource(R.string.position_was, formats.shares(it)) }
                    ?: stringResource(R.string.position_field_shares_hint),
                error = sharesError,
                readOnly = saving,
                imeAction = ImeAction.Done,
                onDone = save,
            )
            Spacer(Modifier.height(16.dp))
            // Read-only: a tap anywhere on the field opens the picker.
            val dateTaps = remember { MutableInteractionSource() }
            LaunchedEffect(dateTaps) {
                dateTaps.interactions.collect { if (it is PressInteraction.Release && !saving) picking = true }
            }
            OutlinedTextField(
                value = times.date(date),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                interactionSource = dateTaps,
                label = {
                    Text(stringResource(if (previous == null) R.string.position_field_date_add else R.string.position_field_date_edit))
                },
                supportingText = {
                    Text(
                        previous?.let { stringResource(R.string.position_field_date_was, times.date(it.lastBuy)) }
                            ?: stringResource(R.string.position_field_date_hint),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { picking = true }, enabled = !saving) {
                        Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.position_pick_date))
                    }
                },
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

    if (picking) {
        val today = LocalDate.now().toEpochDay()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * DAY_MILLIS,
            // A buy is today or earlier.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = Math.floorDiv(utcTimeMillis, DAY_MILLIS) <= today
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { date = LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS)) }
                    picking = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(picker) }
    }
}

@Composable
private fun NumberField(
    state: TextFieldState,
    label: String,
    supporting: String,
    error: Int?,
    readOnly: Boolean,
    imeAction: ImeAction,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text(label) },
        supportingText = { Text(error?.let { stringResource(it) } ?: supporting) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        onKeyboardAction = onDone?.let { done -> KeyboardActionHandler { done() } },
    )
}

/** Calls [onEdit] on every real edit of [state] (not its initial value), so an error goes away once the owner types. */
@Composable
private fun ClearOnEdit(state: TextFieldState, onEdit: () -> Unit) {
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.drop(1).collect { onEdit() } }
}

private val DECIMAL = Regex("""\d+([.,]\d*)?|[.,]\d+""")

/** A positive decimal typed with either a dot or a comma as the decimal mark (decision 0033); null if it is not one. */
internal fun parseDecimal(text: String): Double? =
    text.trim().takeIf { DECIMAL.matches(it) }?.replace(',', '.')?.toDoubleOrNull()
