package app.cozitopia.ui.checklist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPickerDialog(
    date: LocalDate,
    time: LocalTime,
    onConfirm: (LocalDate, LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    if (pickedDate == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDate = dateState.selectedDateMillis?.let {
                            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                        } ?: date
                    },
                ) {
                    Text(strings(StringKey.SAVE))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(strings(StringKey.CANCEL)) } },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(requireNotNull(pickedDate), LocalTime(timeState.hour, timeState.minute))
                        onDismiss()
                    },
                ) {
                    Text(strings(StringKey.SAVE))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(strings(StringKey.CANCEL)) } },
        )
    }
}
