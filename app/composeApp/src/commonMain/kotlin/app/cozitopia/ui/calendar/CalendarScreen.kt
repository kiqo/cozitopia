package app.cozitopia.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.ExportScope
import app.cozitopia.domain.Homie
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.ui.assigneeLabel

private enum class ScopeChoice(val title: StringKey, val fileName: String) {
    OWN(StringKey.SCOPE_OWN, "own"),
    OWN_SELECTED(StringKey.SCOPE_OWN_SELECTED, "own-selected"),
    ALL(StringKey.SCOPE_ALL, "all"),
}

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, myUserId: String, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var exporting by remember { mutableStateOf(false) }
    val occurrences = remember(state.tasks, state.filter) { state.occurrences }
    val others = state.homies.filter { it.userId != myUserId }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings(StringKey.CALENDAR_TITLE),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { exporting = true }) { Text(strings(StringKey.EXPORT_ICS)) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterOption(state.filter, CalendarFilter.All, strings(StringKey.FILTER_ALL), viewModel)
            FilterOption(state.filter, CalendarFilter.Person(myUserId), strings(StringKey.FILTER_ME), viewModel)
            others.forEach { FilterOption(state.filter, CalendarFilter.Person(it.userId), it.name, viewModel) }
            FilterOption(state.filter, CalendarFilter.Everyone, strings(StringKey.EVERYONE), viewModel)
            FilterOption(state.filter, CalendarFilter.Unassigned, strings(StringKey.UNASSIGNED), viewModel)
        }
        if (occurrences.isEmpty()) {
            Text(strings(StringKey.NO_OCCURRENCES))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(occurrences) { occurrence ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "${strings.date(occurrence.date)}  ${occurrence.task.what}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = listOf(
                                    occurrence.task.where,
                                    strings(StringKey.MINUTES, "minutes" to occurrence.task.durationMinutes.toString()),
                                    assigneeLabel(occurrence.task.assignee, state.homies, strings),
                                ).filter { it.isNotBlank() }.joinToString(" - "),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (exporting) {
        ExportDialog(
            others = others,
            onExport = { scope, name -> viewModel.export(scope, name) },
            onDismiss = { exporting = false },
        )
    }
}

@Composable
private fun FilterOption(current: CalendarFilter, option: CalendarFilter, label: String, viewModel: CalendarViewModel) {
    FilterChip(selected = current == option, onClick = { viewModel.filterBy(option) }, label = { Text(label) })
}

@Composable
private fun ExportDialog(others: List<Homie>, onExport: (ExportScope, String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    var choice by remember { mutableStateOf(ScopeChoice.OWN) }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings(StringKey.EXPORT_ICS)) },
        text = {
            Column {
                ScopeChoice.entries.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = choice == option, onClick = { choice = option })
                        Text(strings(option.title))
                    }
                }
                if (choice == ScopeChoice.OWN_SELECTED) {
                    others.forEach { homie ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = homie.userId in selected,
                                onCheckedChange = {
                                    selected = if (homie.userId in selected) selected - homie.userId else selected + homie.userId
                                },
                            )
                            Text(homie.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val scope = when (choice) {
                        ScopeChoice.OWN -> ExportScope.Own
                        ScopeChoice.OWN_SELECTED -> ExportScope.OwnAndSelected(selected)
                        ScopeChoice.ALL -> ExportScope.All
                    }
                    onExport(scope, choice.fileName)
                    onDismiss()
                },
            ) {
                Text(strings(StringKey.EXPORT))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings(StringKey.CANCEL)) } },
    )
}
