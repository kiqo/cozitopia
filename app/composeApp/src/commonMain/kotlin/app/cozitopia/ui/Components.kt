package app.cozitopia.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.cozitopia.data.AppError
import app.cozitopia.domain.Assignee
import app.cozitopia.domain.AssigneeKind
import app.cozitopia.domain.Homie
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings

@Composable
fun <T> DropdownSelect(
    selected: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

fun AppError.text(strings: Strings): String = when (this) {
    is AppError.Known -> strings(key)
    is AppError.Unknown -> message
}

fun assigneeLabel(assignee: Assignee, homies: List<Homie>, strings: Strings): String =
    when (assignee.kind) {
        AssigneeKind.EVERYONE -> strings(StringKey.EVERYONE)
        AssigneeKind.UNASSIGNED -> strings(StringKey.UNASSIGNED)
        AssigneeKind.PEOPLE -> homies.filter { it.userId in assignee.userIds }
            .joinToString { it.name }
            .ifBlank { strings(StringKey.UNASSIGNED) }
    }
