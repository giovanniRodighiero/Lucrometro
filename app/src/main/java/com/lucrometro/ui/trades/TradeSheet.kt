package com.lucrometro.ui.trades

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Quote
import com.lucrometro.data.Replay
import com.lucrometro.data.Side
import com.lucrometro.data.Trade
import com.lucrometro.data.TradeChange
import com.lucrometro.data.replay
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.marketZone
import com.lucrometro.ui.common.parseDecimal
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The add/edit trade sheet (decision 0042): Buy/Sell, date (starts on today), optional time in the exchange's clock,
 * shares (decimals allowed), price per share without fees, and the amount as typed. A change that would take the shares
 * below 0 at any point, replayed with the [others], shows the error on the shares field and disables Save. Editing
 * ([editing] not null) adds Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeSheet(
    code: String,
    editing: Trade?,
    others: List<Trade>,
    quote: Quote,
    formats: QuoteFormats,
    times: TimeFormats,
    onSave: suspend (Trade) -> TradeChange,
    /** Runs in the screen's scope, after the sheet closes, so its snackbar can be seen. */
    onDelete: (Trade) -> Unit,
    onClose: () -> Unit,
) {
    val zone = marketZone(quote.exchangeZone)
    var side by rememberSaveable { mutableStateOf(editing?.side ?: Side.BUY) }
    var date by rememberSaveable { mutableStateOf(editing?.date ?: LocalDate.now(zone)) }
    var time by rememberSaveable { mutableStateOf(editing?.time) }
    val shares = rememberTextFieldState(editing?.quantity?.let(::plain).orEmpty())
    val price = rememberTextFieldState(editing?.price?.let(::plain).orEmpty())
    var sharesError by rememberSaveable { mutableStateOf<Int?>(null) }
    var priceError by rememberSaveable { mutableStateOf<Int?>(null) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThenClose: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }

    val quantity = parseDecimal(shares.text.toString())?.takeIf { it > 0 }
    val unitPrice = parseDecimal(price.text.toString())?.takeIf { it > 0 }
    // Checked as the owner types: the trade as it would be saved, replayed with all the others.
    val candidate = quantity?.let { Trade(editing?.id ?: 0, code, side, date, time, it, unitPrice ?: 0.0, editing?.brokerId) }
    val belowZero = candidate?.let { replay(others + it) as? Replay.BelowZero }

    val save: () -> Unit = save@{
        if (saving) return@save
        sharesError = if (quantity == null) R.string.trade_error_shares else null
        priceError = if (unitPrice == null) R.string.trade_error_price else null
        if (candidate == null || unitPrice == null || belowZero != null) return@save
        saving = true
        saveFailed = false
        scope.launch {
            when (onSave(candidate)) {
                TradeChange.Saved -> hideThenClose()
                // Stored trades changed meanwhile; the live check shows why once they arrive.
                is TradeChange.BelowZero -> saving = false
                TradeChange.Failed -> {
                    saveFailed = true
                    saving = false
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
            Text(
                stringResource(if (editing == null) R.string.trade_sheet_add else R.string.trade_sheet_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Side.entries.forEachIndexed { i, option ->
                    SegmentedButton(
                        selected = side == option,
                        onClick = { side = option },
                        shape = SegmentedButtonDefaults.itemShape(i, Side.entries.size),
                    ) {
                        Text(stringResource(if (option == Side.BUY) R.string.trade_buy else R.string.trade_sell))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PickerField(
                    label = stringResource(R.string.trade_field_date),
                    value = times.date("dMMMyyyy", date),
                    icon = R.drawable.ic_calendar,
                    clickLabel = stringResource(R.string.trade_pick_date),
                    onClick = { pickingDate = true },
                    modifier = Modifier.weight(1f),
                )
                PickerField(
                    label = stringResource(R.string.trade_field_time),
                    value = time?.let(times::time).orEmpty(),
                    icon = R.drawable.ic_schedule,
                    clickLabel = stringResource(R.string.trade_pick_time),
                    onClick = { pickingTime = true },
                    supporting = stringResource(R.string.trade_time_hint),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NumberField(
                    state = shares,
                    label = stringResource(R.string.trade_field_shares),
                    supporting = when {
                        belowZero != null -> stringResource(
                            R.string.trade_error_below_zero,
                            formats.shares(belowZero.sharesBefore),
                            times.date("dMMM", belowZero.trade.date),
                        )
                        else -> sharesError?.let { stringResource(it) }.orEmpty()
                    },
                    error = belowZero != null || sharesError != null,
                    readOnly = saving,
                    imeAction = ImeAction.Next,
                    onAction = null,
                    onEdit = { sharesError = null },
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    state = price,
                    label = quote.currency?.let { stringResource(R.string.trade_field_price_currency, it) }
                        ?: stringResource(R.string.trade_field_price),
                    supporting = priceError?.let { stringResource(it) } ?: stringResource(R.string.trade_price_hint),
                    error = priceError != null,
                    readOnly = saving,
                    imeAction = ImeAction.Done,
                    onAction = save,
                    onEdit = { priceError = null },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    saveFailed -> stringResource(R.string.trade_error_save)
                    quantity != null && unitPrice != null -> stringResource(R.string.trade_amount, formats.price(quote, quantity * unitPrice))
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (saveFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (editing != null) {
                    TextButton(onClick = { hideThenClose(); onDelete(editing) }, enabled = !saving) {
                        Text(stringResource(R.string.trade_delete))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = hideThenClose) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = save,
                    enabled = shares.text.isNotBlank() && price.text.isNotBlank() && belowZero == null && !saving,
                ) {
                    Text(stringResource(if (editing == null) R.string.add else R.string.save))
                }
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker's millis are midnight UTC of the chosen day.
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
    if (pickingTime) {
        val start = time ?: LocalTime.now(zone)
        val state = rememberTimePickerState(start.hour, start.minute, DateFormat.is24HourFormat(LocalContext.current))
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(R.string.trade_pick_time)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    pickingTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        time = null
                        pickingTime = false
                    }) { Text(stringResource(R.string.trade_clear_time)) }
                    TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

/** A read-only field that opens a picker on tap (Doop TextField with a calendar or clock icon). */
@Composable
private fun PickerField(
    label: String,
    value: String,
    icon: Int,
    clickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String = "",
) {
    Box(modifier) {
        OutlinedTextField(
            // Only this overload takes a label position; the text is the picked value, never typed.
            state = remember(value) { TextFieldState(value) },
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            // Floats even when empty (Time), as in the design: a resting label would wrap in a half-width field.
            labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
            label = { Text(label) },
            trailingIcon = { Icon(painterResource(icon), null) },
            supportingText = { Text(supporting) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Over the field, so a tap anywhere opens the picker instead of placing a cursor.
        Box(Modifier.matchParentSize().clickable(onClickLabel = clickLabel, onClick = onClick))
    }
}

@Composable
private fun NumberField(
    state: TextFieldState,
    label: String,
    supporting: String,
    error: Boolean,
    readOnly: Boolean,
    imeAction: ImeAction,
    /** Null keeps the keyboard's default (Next moves to the following field). */
    onAction: (() -> Unit)?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only real edits (not the initial value) clear the error.
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.drop(1).collect { onEdit() } }
    OutlinedTextField(
        state = state,
        modifier = modifier,
        readOnly = readOnly,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text(label) },
        supportingText = { Text(supporting) },
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        onKeyboardAction = { default -> onAction?.invoke() ?: default() },
    )
}

/** A number as plain text for a field to edit ("2.5", never "2.50" or "1,000"): it parses back the same. */
private fun plain(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
