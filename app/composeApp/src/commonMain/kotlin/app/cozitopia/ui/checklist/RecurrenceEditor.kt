package app.cozitopia.ui.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.Ordinal
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings
import app.cozitopia.i18n.key
import app.cozitopia.ui.DropdownSelect
import kotlinx.datetime.DayOfWeek

private val NUMBER_FIELD_WIDTH = 88.dp

fun RepeatMode.key(): StringKey = when (this) {
    RepeatMode.ORDINAL_WEEKDAY -> StringKey.REPEAT_MODE_ORDINAL
    RepeatMode.WEEKDAY -> StringKey.REPEAT_MODE_WEEKDAY
    RepeatMode.DATE -> StringKey.REPEAT_MODE_DATE
}

fun describeRecurrence(recurrence: Recurrence, strings: Strings): String {
    val prefix = "${strings(StringKey.EVERY)} ${recurrence.interval} ${strings(recurrence.unit.key())}"
    val detail = listOfNotNull(
        recurrence.ordinal?.let { strings(it.key()) },
        recurrence.weekday?.let { strings(it.key()) },
        recurrence.dayOfMonth?.let { "${strings(StringKey.DAY_OF_MONTH)} $it" },
        recurrence.month?.let { "${strings(StringKey.MONTH)} $it" },
    ).joinToString(" ")
    return if (detail.isEmpty()) prefix else "$prefix, $detail"
}

@Composable
fun RecurrenceEditorDialog(
    recurrence: Recurrence,
    onChange: (Recurrence) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var draft by remember { mutableStateOf(RecurrenceDraft.from(recurrence)) }
    fun update(transform: (RecurrenceDraft) -> RecurrenceDraft) {
        draft = transform(draft)
        onChange(RecurrenceDraft.normalized(draft).toRecurrence())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings(StringKey.SAVE)) } },
        title = { Text(strings(StringKey.COL_REPETITION)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.mode == mode,
                            onClick = { update { it.copy(mode = mode) } },
                            label = { Text(strings(mode.key())) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings(StringKey.EVERY))
                    OutlinedTextField(
                        value = draft.interval.toString(),
                        onValueChange = { text -> text.toIntOrNull()?.let { value -> update { it.copy(interval = value) } } },
                        singleLine = true,
                        modifier = Modifier.width(NUMBER_FIELD_WIDTH),
                    )
                    DropdownSelect(
                        selected = strings(draft.unit.key()),
                        options = RecurrenceUnit.entries,
                        optionLabel = { strings(it.key()) },
                        onSelect = { unit -> update { it.copy(unit = unit) } },
                    )
                }
                if (draft.mode == RepeatMode.ORDINAL_WEEKDAY) {
                    DropdownSelect(
                        selected = strings(draft.ordinal.key()),
                        options = Ordinal.entries,
                        optionLabel = { strings(it.key()) },
                        onSelect = { ordinal -> update { it.copy(ordinal = ordinal) } },
                    )
                }
                if (draft.mode != RepeatMode.DATE) {
                    DropdownSelect(
                        selected = strings(draft.weekday.key()),
                        options = DayOfWeek.entries,
                        optionLabel = { strings(it.key()) },
                        onSelect = { weekday -> update { it.copy(weekday = weekday) } },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (draft.mode == RepeatMode.DATE) {
                        OutlinedTextField(
                            value = draft.dayOfMonth.toString(),
                            onValueChange = { text ->
                                text.toIntOrNull()?.let { value -> update { it.copy(dayOfMonth = value) } }
                            },
                            label = { Text(strings(StringKey.DAY_OF_MONTH)) },
                            singleLine = true,
                            modifier = Modifier.width(NUMBER_FIELD_WIDTH),
                        )
                    }
                    OutlinedTextField(
                        value = draft.month?.toString().orEmpty(),
                        onValueChange = { text -> update { it.copy(month = text.toIntOrNull()) } },
                        label = { Text(strings(StringKey.MONTH)) },
                        singleLine = true,
                        modifier = Modifier.width(NUMBER_FIELD_WIDTH),
                    )
                }
            }
        },
    )
}
