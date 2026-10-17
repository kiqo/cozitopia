package app.cozitopia.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.Homie
import app.cozitopia.domain.Schedule
import app.cozitopia.domain.TaskPlanner
import app.cozitopia.domain.TaskWarning
import app.cozitopia.domain.Workload
import app.cozitopia.domain.WorkloadSummary
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings
import app.cozitopia.ui.assigneeLabel
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readString

private val WHAT_WIDTH = 200.dp
private val WHERE_WIDTH = 150.dp
private val FREQUENCY_WIDTH = 130.dp
private val DURATION_WIDTH = 110.dp
private val ASSIGNEE_WIDTH = 190.dp
private val REPETITION_WIDTH = 230.dp
private val DATE_WIDTH = 130.dp
private val TIME_WIDTH = 90.dp
private val DELETE_WIDTH = 56.dp
private val WARNING_WIDTH = 300.dp

private fun warningKey(warning: TaskWarning): StringKey = when (warning) {
    TaskWarning.UNKNOWN_ASSIGNEE -> StringKey.WARN_UNKNOWN_ASSIGNEE
    TaskWarning.INVALID_DURATION -> StringKey.WARN_INVALID_DURATION
    TaskWarning.UNRECOGNIZED_FREQUENCY -> StringKey.WARN_UNRECOGNIZED_FREQUENCY
    TaskWarning.EXCEEDS_DAY -> StringKey.WARN_EXCEEDS_DAY
}

@Composable
fun ChecklistScreen(viewModel: ChecklistViewModel, onOpenCalendar: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val picker = rememberFilePickerLauncher(type = FileKitType.File("csv")) { file ->
        file?.let { picked -> viewModel.importCsv { picked.readString() } }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = strings(StringKey.CHECKLIST_TITLE), style = MaterialTheme.typography.titleLarge)
        Text(text = strings(StringKey.CHECKLIST_HELP), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::downloadTemplate) { Text(strings(StringKey.DOWNLOAD_TEMPLATE)) }
            Button(onClick = { picker.launch() }) { Text(strings(StringKey.UPLOAD_CSV)) }
        }
        if (state.skippedLines.isNotEmpty()) {
            Text(
                text = strings(StringKey.SKIPPED_LINES, "lines" to state.skippedLines.joinToString()),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.tasks.isNotEmpty()) {
            TaskTable(state.tasks, state.homies, state.importCount, viewModel, strings)
            WorkloadSummaryCard(WorkloadSummary.of(state.tasks, state.homies), strings)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::assignAll) { Text(strings(StringKey.ASSIGN_ALL)) }
                OutlinedButton(onClick = viewModel::refreshDates) { Text(strings(StringKey.REFRESH_DATES)) }
                Button(onClick = viewModel::finish, enabled = state.canFinish) {
                    Text(strings(StringKey.FINISH_CHECKLIST))
                }
            }
            if (!state.canFinish) {
                Text(text = strings(StringKey.FINISH_BLOCKED), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (state.finished) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinished,
            text = { Text(strings(StringKey.CHECKLIST_DONE)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissFinished()
                        onOpenCalendar()
                    },
                ) {
                    Text(strings(StringKey.YES))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissFinished) { Text(strings(StringKey.NO)) } },
        )
    }
}

@Composable
private fun TaskTable(
    tasks: List<ChoreTask>,
    homies: List<Homie>,
    importCount: Int,
    viewModel: ChecklistViewModel,
    strings: Strings,
) {
    Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderCell(StringKey.COL_WHAT, WHAT_WIDTH)
            HeaderCell(StringKey.COL_WHERE, WHERE_WIDTH)
            HeaderCell(StringKey.COL_FREQUENCY, FREQUENCY_WIDTH)
            HeaderCell(StringKey.COL_DURATION, DURATION_WIDTH)
            HeaderCell(StringKey.COL_ASSIGNEE, ASSIGNEE_WIDTH)
            HeaderCell(StringKey.COL_REPETITION, REPETITION_WIDTH)
            HeaderCell(StringKey.COL_NEXT_DATE, DATE_WIDTH)
            HeaderCell(StringKey.COL_START_TIME, TIME_WIDTH)
            HeaderCell(StringKey.DELETE, DELETE_WIDTH)
        }
        tasks.forEach { task -> key(importCount, task.id) { TaskRow(task, homies, viewModel, strings) } }
    }
}

@Composable
private fun WorkloadSummaryCard(workloads: List<Workload>, strings: Strings) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = strings(StringKey.SUMMARY_TITLE), style = MaterialTheme.typography.titleMedium)
        workloads.forEach { workload ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = workload.homie?.name ?: strings(StringKey.UNASSIGNED),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(strings(StringKey.SUMMARY_YEARLY, "minutes" to strings(StringKey.MINUTES, "minutes" to workload.yearlyMinutes.toString())))
                    Text(strings(StringKey.SUMMARY_MONTHLY, "minutes" to strings(StringKey.MINUTES, "minutes" to workload.monthlyMinutes.toString())))
                    Text(strings(StringKey.SUMMARY_TASKS, "count" to workload.taskCount.toString()))
                    Text(
                        if (workload.rooms.isEmpty()) {
                            strings(StringKey.SUMMARY_NO_ROOMS)
                        } else {
                            strings(StringKey.SUMMARY_ROOMS, "rooms" to workload.rooms.joinToString())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(title: StringKey, width: Dp) {
    Text(
        text = LocalStrings.current(title),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.width(width),
    )
}

@Composable
private fun TaskRow(task: ChoreTask, homies: List<Homie>, viewModel: ChecklistViewModel, strings: Strings) {
    var durationText by remember { mutableStateOf(if (task.durationMinutes == 0) "" else "${task.durationMinutes}min") }
    var editingRecurrence by remember { mutableStateOf(false) }
    
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = task.what,
            onValueChange = { viewModel.setWhat(task.id, it) },
            singleLine = true,
            modifier = Modifier.width(WHAT_WIDTH),
        )
        OutlinedTextField(
            value = task.where,
            onValueChange = { viewModel.setWhere(task.id, it) },
            singleLine = true,
            modifier = Modifier.width(WHERE_WIDTH),
        )
        OutlinedTextField(
            value = task.frequencyText,
            onValueChange = { viewModel.setFrequency(task.id, it) },
            isError = TaskWarning.UNRECOGNIZED_FREQUENCY in task.warnings,
            singleLine = true,
            modifier = Modifier.width(FREQUENCY_WIDTH),
        )
        OutlinedTextField(
            value = durationText,
            onValueChange = {
                durationText = it
                viewModel.setDuration(task.id, it)
            },
            isError = TaskWarning.INVALID_DURATION in task.warnings,
            singleLine = true,
            modifier = Modifier.width(DURATION_WIDTH),
        )
        AssigneeCell(task, homies, viewModel, strings, Modifier.width(ASSIGNEE_WIDTH))
        OutlinedButton(onClick = { editingRecurrence = true }, modifier = Modifier.width(REPETITION_WIDTH)) {
            Text(task.recurrence?.let { describeRecurrence(it, strings) } ?: strings(StringKey.SET_REPETITION))
        }
        Text(text = task.nextDate?.let(strings::date).orEmpty(), modifier = Modifier.width(DATE_WIDTH))
        Text(
            text = task.startTime?.takeIf { task.durationMinutes > 0 }?.toString().orEmpty(),
            modifier = Modifier.width(TIME_WIDTH),
        )
        IconButton(onClick = { viewModel.delete(task.id) }, modifier = Modifier.width(DELETE_WIDTH)) {
            Icon(Icons.Filled.Delete, contentDescription = strings(StringKey.DELETE))
        }
        Text(
            text = task.warnings.joinToString { strings(warningKey(it), "text" to task.frequencyText) },
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.width(WARNING_WIDTH),
        )
    }

    if (editingRecurrence) {
        RecurrenceEditorDialog(
            recurrence = task.recurrence ?: Schedule.defaultRecurrence(TaskPlanner.windowFor(task.assignee, homies).weekday),
            onChange = { viewModel.setRecurrence(task.id, it) },
            onDismiss = { editingRecurrence = false },
        )
    }
}

private fun Modifier.highlighted(condition: Boolean, color: Color): Modifier =
    if (condition) background(color) else this

@Composable
private fun AssigneeCell(
    task: ChoreTask,
    homies: List<Homie>,
    viewModel: ChecklistViewModel,
    strings: Strings,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(AssigneeOption.Everyone, AssigneeOption.Unassigned) + homies.map { AssigneeOption.Person(it.userId) }
    val hasWarning = TaskWarning.UNKNOWN_ASSIGNEE in task.warnings
    Column(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().highlighted(hasWarning, MaterialTheme.colorScheme.errorContainer),
        ) {
            Text(assigneeLabel(task.assignee, homies, strings))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected(task.assignee, option), onCheckedChange = null)
                            Text(optionLabel(option, homies, strings))
                        }
                    },
                    onClick = { viewModel.setAssignee(task.id, toggleAssignee(task.assignee, option)) },
                )
            }
        }
    }
}

private fun optionLabel(option: AssigneeOption, homies: List<Homie>, strings: Strings): String = when (option) {
    AssigneeOption.Everyone -> strings(StringKey.EVERYONE)
    AssigneeOption.Unassigned -> strings(StringKey.UNASSIGNED)
    is AssigneeOption.Person -> homies.first { it.userId == option.userId }.name
}
