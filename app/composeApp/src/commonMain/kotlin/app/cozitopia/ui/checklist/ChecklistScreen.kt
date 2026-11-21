package app.cozitopia.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.AssigneeKind
import app.cozitopia.domain.ChecklistCsv
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.DayWindow
import app.cozitopia.domain.Homie
import app.cozitopia.domain.Schedule
import app.cozitopia.domain.TaskPlanner
import app.cozitopia.domain.TaskWarning
import app.cozitopia.domain.Workload
import app.cozitopia.domain.WorkloadSummary
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings
import app.cozitopia.ui.CreamCard
import app.cozitopia.ui.EmptyState
import app.cozitopia.ui.FieldLabel
import app.cozitopia.ui.HintText
import app.cozitopia.ui.LabeledTextField
import app.cozitopia.ui.LocalWideLayout
import app.cozitopia.ui.MenuAction
import app.cozitopia.ui.MenuButton
import app.cozitopia.ui.MinTouchTarget
import app.cozitopia.ui.MoreButton
import app.cozitopia.ui.NeutralTone
import app.cozitopia.ui.PageHeader
import app.cozitopia.ui.PrimaryButton
import app.cozitopia.ui.ScreenColumn
import app.cozitopia.ui.SecondaryButton
import app.cozitopia.ui.SectionCard
import app.cozitopia.ui.SoftChip
import app.cozitopia.ui.TertiaryButton
import app.cozitopia.ui.assigneeLabel
import app.cozitopia.ui.today
import app.cozitopia.ui.toneOf
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readString
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

private enum class TaskFilter(val label: StringKey, val matches: (ChoreTask) -> Boolean) {
    ALL(StringKey.FILTER_ALL_TASKS, { true }),
    UNASSIGNED(StringKey.UNASSIGNED, { it.assignee.kind == AssigneeKind.UNASSIGNED }),
    PINNED(StringKey.FILTER_PINNED, { it.pinned }),
    NEEDS_LOOK(StringKey.FILTER_NEEDS_LOOK, { it.warnings.isNotEmpty() || it.recurrence == null }),
}

private val MONTH_SHORT_KEYS = listOf(
    StringKey.MONTH_1, StringKey.MONTH_2, StringKey.MONTH_3, StringKey.MONTH_4,
    StringKey.MONTH_5, StringKey.MONTH_6, StringKey.MONTH_7, StringKey.MONTH_8,
    StringKey.MONTH_9, StringKey.MONTH_10, StringKey.MONTH_11, StringKey.MONTH_12,
)
private const val SHORT_MONTH_LENGTH = 3

private fun Strings.shortDate(date: LocalDate): String =
    "${date.day} ${this(MONTH_SHORT_KEYS[date.month.number - 1]).take(SHORT_MONTH_LENGTH)}"

private fun Strings.minutes(value: Int): String = this(StringKey.MINUTES, "minutes" to value.toString())

private fun warningKey(warning: TaskWarning): StringKey = when (warning) {
    TaskWarning.UNKNOWN_ASSIGNEE -> StringKey.WARN_UNKNOWN_ASSIGNEE
    TaskWarning.INVALID_DURATION -> StringKey.WARN_INVALID_DURATION
    TaskWarning.UNRECOGNIZED_FREQUENCY -> StringKey.WARN_UNRECOGNIZED_FREQUENCY
    TaskWarning.EXCEEDS_DAY -> StringKey.WARN_EXCEEDS_DAY
    TaskWarning.INVALID_START -> StringKey.WARN_INVALID_START
}

private fun warningText(task: ChoreTask, strings: Strings): String =
    task.warnings.joinToString(" ") { strings(warningKey(it), "text" to task.frequencyText) }

private class TaskActions(
    val viewModel: ChecklistViewModel,
    val homies: List<Homie>,
    val myUserId: String,
    val onEdit: (ChoreTask) -> Unit,
)

@Composable
fun ChecklistScreen(viewModel: ChecklistViewModel, myUserId: String, onOpenCalendar: () -> Unit) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    var helpExpanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var editingId by remember { mutableStateOf<String?>(null) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File("csv")) { file ->
        file?.let { picked -> viewModel.importCsv { picked.readString() } }
    }
    val wide = LocalWideLayout.current
    val actions = TaskActions(viewModel, state.homies, myUserId) { editingId = it.id }
    val visibleTasks = state.tasks.filter(filter.matches)
    val csvActions = listOf(
        MenuAction(strings(StringKey.DOWNLOAD_TEMPLATE), onClick = viewModel::downloadTemplate),
        MenuAction(strings(StringKey.UPLOAD_CSV)) { picker.launch() },
        MenuAction(strings(StringKey.EXPORT_CSV), enabled = state.tasks.isNotEmpty(), onClick = viewModel::exportChecklist),
        MenuAction(strings(StringKey.CHECKLIST_HELP_TOGGLE)) { helpExpanded = !helpExpanded },
    )

    ScreenColumn {
        PageHeader(strings(StringKey.CHECKLIST_TITLE), strings(StringKey.SUBTITLE_CHECKLIST)) { modifier ->
            if (state.tasks.isEmpty()) {
                PrimaryButton(strings(StringKey.UPLOAD_CSV), { picker.launch() }, modifier)
            } else {
                PrimaryButton(strings(StringKey.ASSIGN_ALL), viewModel::assignAll, modifier)
            }
        }
        if (state.skippedLines.isNotEmpty()) {
            Text(
                strings(StringKey.SKIPPED_LINES, "lines" to state.skippedLines.joinToString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.tasks.isEmpty()) {
            EmptyState(Icons.Outlined.Spa, strings(StringKey.CHECKLIST_EMPTY_TITLE), strings(StringKey.CHECKLIST_EMPTY_BODY)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(strings(StringKey.DOWNLOAD_TEMPLATE), viewModel::downloadTemplate)
                    TertiaryButton(strings(StringKey.CHECKLIST_HELP_TOGGLE), { helpExpanded = !helpExpanded })
                }
            }
            if (helpExpanded) CsvHelp(strings)
            return@ScreenColumn
        }
        WorkloadCard(WorkloadSummary.of(state.tasks, state.homies), myUserId)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MenuButton(strings(StringKey.CSV_TOOLS), csvActions, Modifier.weight(1f, fill = !wide))
            MenuButton(
                strings(filter.label),
                TaskFilter.entries.map { option -> MenuAction(strings(option.label)) { filter = option } },
                Modifier.weight(1f, fill = !wide),
            )
            if (wide) {
                Spacer(Modifier.weight(2f))
                HintText(
                    strings(
                        StringKey.TASK_COUNT,
                        "count" to state.tasks.size.toString(),
                        "unassigned" to state.tasks.count(TaskFilter.UNASSIGNED.matches).toString(),
                    ),
                )
            }
        }
        if (helpExpanded) CsvHelp(strings)
        if (visibleTasks.isEmpty()) {
            HintText(strings(StringKey.NO_MATCHING_TASKS))
        } else if (wide) {
            TaskTable(visibleTasks, state.importCount, actions)
        } else {
            visibleTasks.forEach { task -> key(state.importCount, task.id) { TaskCard(task, actions) } }
        }
        ChecklistFooter(state.canFinish, viewModel)
    }

    state.tasks.firstOrNull { it.id == editingId }?.let { task ->
        key(state.importCount, task.id) { TaskEditDialog(task, actions) { editingId = null } }
    }

    if (state.finished) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinished,
            containerColor = CreamCard,
            shape = MaterialTheme.shapes.extraLarge,
            text = { Text(strings(StringKey.CHECKLIST_DONE), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                PrimaryButton(strings(StringKey.YES), {
                    viewModel.dismissFinished()
                    onOpenCalendar()
                })
            },
            dismissButton = { TertiaryButton(strings(StringKey.NO), viewModel::dismissFinished) },
        )
    }
}

@Composable
private fun CsvHelp(strings: Strings) {
    SectionCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, bordered = false, padding = 20.dp) {
        Text(strings(StringKey.CHECKLIST_HELP), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChecklistFooter(canFinish: Boolean, viewModel: ChecklistViewModel) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            HintText(strings(StringKey.PIN_HINT), Modifier.weight(1f).widthIn(min = 200.dp))
            TertiaryButton(strings(StringKey.REFRESH_DATES), viewModel::refreshDates)
            SecondaryButton(strings(StringKey.FINISH_CHECKLIST), viewModel::finish, enabled = canFinish)
        }
        if (!canFinish) {
            Text(strings(StringKey.FINISH_BLOCKED), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WorkloadCard(workloads: List<Workload>, myUserId: String) {
    val strings = LocalStrings.current
    val homies = workloads.mapNotNull { it.homie }
    val total = workloads.sumOf { it.monthlyMinutes }
    val max = workloads.maxOfOrNull { it.monthlyMinutes }?.coerceAtLeast(1) ?: 1
    fun name(workload: Workload): String = workload.homie?.let {
        if (it.userId == myUserId) "${it.name} · ${strings(StringKey.YOU)}" else it.name
    } ?: strings(StringKey.UNASSIGNED)
    fun tone(workload: Workload) = workload.homie?.let { toneOf(it.userId, homies) } ?: NeutralTone

    SectionCard(Modifier.fillMaxWidth(), bordered = false, padding = 20.dp) {
        if (LocalWideLayout.current) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.width(168.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(strings(StringKey.SHARED_EFFORT), style = MaterialTheme.typography.titleMedium)
                    HintText(strings(StringKey.EFFORT_PER_MONTH, "minutes" to total.toString()))
                }
                workloads.forEach { workload ->
                    Column(Modifier.width(140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name(workload), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(strings.minutes(workload.monthlyMinutes), style = MaterialTheme.typography.titleLarge.copy(fontFamily = MaterialTheme.typography.titleMedium.fontFamily))
                        Box(Modifier.fillMaxWidth().heightIn(min = 6.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainer)) {
                            Box(
                                Modifier
                                    .fillMaxWidth(workload.monthlyMinutes.toFloat() / max)
                                    .heightIn(min = 6.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(tone(workload).content.copy(alpha = 0.4f)),
                            )
                        }
                        HintText(strings(StringKey.SUMMARY_TASKS, "count" to workload.taskCount.toString()))
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings(StringKey.SHARED_EFFORT), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                HintText(strings(StringKey.EFFORT_PER_MONTH, "minutes" to total.toString()))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                workloads.filter { it.homie != null }.forEach { workload ->
                    SoftChip("${name(workload)}  ${strings.minutes(workload.monthlyMinutes)}", tone(workload))
                }
            }
            workloads.firstOrNull { it.homie == null }?.let {
                HintText(strings(StringKey.UNASSIGNED_LEFT, "minutes" to it.monthlyMinutes.toString()))
            }
        }
    }
}

private val PinWidth = 48.dp
private val EditWidth = 64.dp

@Composable
private fun RowScope.Cell(weight: Float, content: @Composable () -> Unit) {
    Box(Modifier.weight(weight).padding(end = 12.dp), contentAlignment = Alignment.CenterStart) { content() }
}

@Composable
private fun CellText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun TaskTable(tasks: List<ChoreTask>, importCount: Int, actions: TaskActions) {
    val strings = LocalStrings.current
    SectionCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(PinWidth), contentAlignment = Alignment.Center) { HeaderText(strings(StringKey.COL_PIN)) }
                Cell(1.7f) { HeaderText(strings(StringKey.COL_WHAT)) }
                Cell(1.1f) { HeaderText(strings(StringKey.COL_WHERE)) }
                Cell(1f) { HeaderText(strings(StringKey.COL_FREQUENCY)) }
                Cell(0.8f) { HeaderText(strings(StringKey.COL_DURATION)) }
                Cell(1.3f) { HeaderText(strings(StringKey.COL_ASSIGNEE)) }
                Cell(1.3f) { HeaderText(strings(StringKey.COL_REPETITION)) }
                Cell(1f) { HeaderText(strings(StringKey.COL_NEXT_DATE)) }
                Box(Modifier.width(EditWidth), contentAlignment = Alignment.Center) { HeaderText(strings(StringKey.COL_EDIT)) }
            }
            tasks.forEachIndexed { index, task ->
                key(importCount, task.id) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    TaskRow(task, index % 2 == 1, actions)
                }
            }
        }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
}

@Composable
private fun TaskRow(task: ChoreTask, striped: Boolean, actions: TaskActions) {
    val strings = LocalStrings.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val background = when {
        hovered -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
        striped -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).background(background).hoverable(source).padding(horizontal = 8.dp),
    ) {
        Box(Modifier.width(PinWidth), contentAlignment = Alignment.Center) { PinToggle(task, actions) }
        Cell(1.7f) { TaskTitle(task, MaterialTheme.typography.bodyMedium) }
        Cell(1.1f) { CellText(task.where) }
        Cell(1f) { CellText(task.frequencyText, if (TaskWarning.UNRECOGNIZED_FREQUENCY in task.warnings) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
        Cell(0.8f) { CellText(strings.minutes(task.durationMinutes)) }
        Cell(1.3f) { AssigneeChip(task, actions) }
        Cell(1.3f) { RepetitionText(task) }
        Cell(1f) { CellText(task.nextDate?.let { strings.shortDate(it) }.orEmpty()) }
        Box(Modifier.width(EditWidth), contentAlignment = Alignment.Center) { TaskMenu(task, actions) }
    }
}

@Composable
private fun TaskTitle(task: ChoreTask, style: androidx.compose.ui.text.TextStyle) {
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(task.what, style = style, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (task.warnings.isNotEmpty()) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = warningText(task, strings),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun RepetitionText(task: ChoreTask) {
    val strings = LocalStrings.current
    val recurrence = task.recurrence
    if (recurrence == null) {
        CellText(strings(StringKey.SET_REPETITION), MaterialTheme.colorScheme.error)
    } else {
        CellText(describeRecurrence(recurrence, strings))
    }
}

@Composable
private fun AssigneeChip(task: ChoreTask, actions: TaskActions) {
    val strings = LocalStrings.current
    val label = assigneeLabel(task.assignee, actions.homies, strings).let {
        if (task.assignee.userIds == setOf(actions.myUserId)) "$it · ${strings(StringKey.YOU)}" else it
    }
    SoftChip(label, toneOf(task.assignee, actions.homies))
}

@Composable
private fun PinToggle(task: ChoreTask, actions: TaskActions) {
    val strings = LocalStrings.current
    if (task.pinned) {
        IconButton(onClick = { actions.viewModel.unpin(task.id) }) {
            Icon(Icons.Filled.PushPin, contentDescription = strings(StringKey.UNPIN), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    } else {
        Icon(
            Icons.Outlined.PushPin,
            contentDescription = strings(StringKey.NOT_PINNED),
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun TaskMenu(task: ChoreTask, actions: TaskActions) {
    val strings = LocalStrings.current
    MoreButton(
        strings(StringKey.MORE_ACTIONS),
        listOfNotNull(
            MenuAction(strings(StringKey.EDIT_TASK)) { actions.onEdit(task) },
            if (task.pinned) MenuAction(strings(StringKey.UNPIN)) { actions.viewModel.unpin(task.id) } else null,
            MenuAction(strings(StringKey.DELETE)) { actions.viewModel.delete(task.id) },
        ),
    )
}

@Composable
private fun TaskCard(task: ChoreTask, actions: TaskActions) {
    val strings = LocalStrings.current
    SectionCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TaskTitle(task, MaterialTheme.typography.titleMedium)
                Text(
                    listOf(task.where, strings.minutes(task.durationMinutes)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TaskMenu(task, actions)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssigneeChip(task, actions)
            if (task.pinned) SoftChip(strings(StringKey.PINNED), NeutralTone, bordered = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel(strings(StringKey.COL_FREQUENCY))
                Text(task.frequencyText, style = MaterialTheme.typography.bodyMedium)
            }
            Column(Modifier.weight(1f)) {
                FieldLabel(strings(StringKey.COL_REPETITION))
                RepetitionText(task)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val start = listOfNotNull(task.nextDate?.let { strings.shortDate(it) }, task.startTime?.toString()).joinToString(", ")
        Text(
            "${strings(StringKey.NEXT_START)}  $start",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = "${strings(StringKey.NEXT_START)} $start" },
        )
    }
}

@Composable
private fun TaskEditDialog(task: ChoreTask, actions: TaskActions, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val viewModel = actions.viewModel
    var durationText by remember { mutableStateOf(if (task.durationMinutes == 0) "" else "${task.durationMinutes}min") }
    var editingRecurrence by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CreamCard,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(strings(StringKey.EDIT_TASK), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledTextField(strings(StringKey.COL_WHAT), task.what, { viewModel.setWhat(task.id, it) })
                LabeledTextField(strings(StringKey.COL_WHERE), task.where, { viewModel.setWhere(task.id, it) })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledTextField(
                        label = strings(StringKey.COL_FREQUENCY),
                        value = task.frequencyText,
                        onValueChange = { viewModel.setFrequency(task.id, it) },
                        isError = TaskWarning.UNRECOGNIZED_FREQUENCY in task.warnings,
                        modifier = Modifier.weight(1f),
                    )
                    LabeledTextField(
                        label = strings(StringKey.COL_DURATION),
                        value = durationText,
                        onValueChange = {
                            durationText = it
                            viewModel.setDuration(task.id, it)
                        },
                        isError = TaskWarning.INVALID_DURATION in task.warnings,
                        modifier = Modifier.weight(1f),
                    )
                }
                AssigneePicker(task, actions)
                Column {
                    FieldLabel(strings(StringKey.COL_REPETITION))
                    SecondaryButton(
                        task.recurrence?.let { describeRecurrence(it, strings) } ?: strings(StringKey.SET_REPETITION),
                        { editingRecurrence = true },
                        Modifier.fillMaxWidth(),
                    )
                }
                Column {
                    FieldLabel(strings(StringKey.COL_NEXT_DATE))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SecondaryButton(
                            listOfNotNull(task.nextDate?.let(strings::date), task.startTime?.toString()).joinToString(" "),
                            { pickingStart = true },
                            Modifier.weight(1f),
                            enabled = task.recurrence != null,
                        )
                        if (task.pinned) {
                            IconButton(onClick = { viewModel.unpin(task.id) }) {
                                Icon(Icons.Filled.PushPin, contentDescription = strings(StringKey.UNPIN), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (task.warnings.isNotEmpty()) {
                    Text(warningText(task, strings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { PrimaryButton(strings(StringKey.DONE), onDismiss) },
        dismissButton = {
            TertiaryButton(strings(StringKey.DELETE), {
                onDismiss()
                viewModel.delete(task.id)
            })
        },
    )

    if (editingRecurrence) {
        RecurrenceEditorDialog(
            recurrence = task.recurrence ?: Schedule.defaultRecurrence(TaskPlanner.windowFor(task.assignee, actions.homies).weekday),
            onChange = { viewModel.setRecurrence(task.id, it) },
            onDismiss = { editingRecurrence = false },
        )
    }
    if (pickingStart) {
        StartPickerDialog(
            date = task.nextDate ?: today(),
            time = task.startTime ?: DayWindow.DEFAULT.start,
            onConfirm = { pickedDate, pickedTime -> viewModel.setStart(task.id, pickedDate, pickedTime) },
            onDismiss = { pickingStart = false },
        )
    }
}

@Composable
private fun AssigneePicker(task: ChoreTask, actions: TaskActions) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(AssigneeOption.Everyone, AssigneeOption.Unassigned) + actions.homies.map { AssigneeOption.Person(it.userId) }
    Column {
        FieldLabel(strings(StringKey.COL_ASSIGNEE))
        Box {
            SecondaryButton(assigneeLabel(task.assignee, actions.homies, strings), { expanded = true }, Modifier.fillMaxWidth())
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = CreamCard, shape = MaterialTheme.shapes.large) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isSelected(task.assignee, option), onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(optionLabel(option, actions.homies, strings))
                            }
                        },
                        onClick = { actions.viewModel.setAssignee(task.id, toggleAssignee(task.assignee, option)) },
                        modifier = Modifier.heightIn(min = MinTouchTarget),
                    )
                }
            }
        }
    }
}

private fun optionLabel(option: AssigneeOption, homies: List<Homie>, strings: Strings): String = when (option) {
    AssigneeOption.Everyone -> strings(StringKey.EVERYONE)
    AssigneeOption.Unassigned -> strings(StringKey.UNASSIGNED)
    is AssigneeOption.Person -> homies.first { it.userId == option.userId }.name
}
