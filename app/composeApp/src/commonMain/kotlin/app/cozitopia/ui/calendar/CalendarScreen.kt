package app.cozitopia.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.ExportScope
import app.cozitopia.domain.Homie
import app.cozitopia.domain.IcsExporter
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings
import app.cozitopia.i18n.key
import app.cozitopia.ui.CreamCard
import app.cozitopia.ui.EmptyState
import app.cozitopia.ui.HintText
import app.cozitopia.ui.LocalWideLayout
import app.cozitopia.ui.MinTouchTarget
import app.cozitopia.ui.PageHeader
import app.cozitopia.ui.PrimaryButton
import app.cozitopia.ui.SelectField
import app.cozitopia.ui.SectionCard
import app.cozitopia.ui.SoftChip
import app.cozitopia.ui.TertiaryButton
import app.cozitopia.ui.assigneeLabel
import app.cozitopia.ui.today
import app.cozitopia.ui.toneOf
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

private val MONTH_KEYS = listOf(
    StringKey.MONTH_1, StringKey.MONTH_2, StringKey.MONTH_3, StringKey.MONTH_4,
    StringKey.MONTH_5, StringKey.MONTH_6, StringKey.MONTH_7, StringKey.MONTH_8,
    StringKey.MONTH_9, StringKey.MONTH_10, StringKey.MONTH_11, StringKey.MONTH_12,
)
private const val MINUTES_PER_DAY = 24 * 60
private const val DAYS_PER_WEEK = 7

private fun Strings.monthName(date: LocalDate): String = this(MONTH_KEYS[date.month.number - 1])

private fun Strings.monthTitle(date: LocalDate): String =
    "${monthName(date).replaceFirstChar { it.uppercase() }} ${date.year}"

private fun timeRange(task: ChoreTask): String? {
    val start = task.startTime?.takeIf { task.durationMinutes > 0 } ?: return null
    val endMinutes = (start.hour * 60 + start.minute + task.durationMinutes).coerceAtMost(MINUTES_PER_DAY - 1)
    return "$start–${LocalTime(endMinutes / 60, endMinutes % 60)}"
}

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, myUserId: String) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var exporting by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val occurrences = remember(state.tasks, state.filter) { state.occurrences }
    val byDate = remember(occurrences) { occurrences.groupBy { it.date } }
    val wide = LocalWideLayout.current
    val horizontal = if (wide) 48.dp else 20.dp
    val agendaState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val dayIndex = remember(byDate) {
        buildMap {
            var index = 0
            var lastMonth: Pair<Int, Int>? = null
            byDate.forEach { (date, dayOccurrences) ->
                val month = date.year to date.month.number
                if (month != lastMonth) {
                    lastMonth = month
                    index++
                }
                put(date, index)
                index += 1 + dayOccurrences.size
            }
        }
    }
    val jumpTo: (LocalDate) -> Unit = { date ->
        selectedDate = date
        (dayIndex.entries.firstOrNull { it.key >= date }?.value ?: dayIndex.values.lastOrNull())?.let {
            scope.launch { agendaState.animateScrollToItem(it) }
        }
    }

    val header: LazyListScope.() -> Unit = {
        item {
            PageHeader(strings(StringKey.CALENDAR_TITLE), strings(StringKey.SUBTITLE_CALENDAR)) { modifier ->
                PrimaryButton(strings(StringKey.EXPORT_ICS), { exporting = true }, modifier)
            }
        }
        item { FilterRow(state, myUserId, viewModel) }
    }
    val agenda: LazyListScope.() -> Unit = {
        if (byDate.isEmpty()) {
            item {
                EmptyState(Icons.Outlined.EventAvailable, strings(StringKey.CALENDAR_EMPTY_TITLE), strings(StringKey.NO_OCCURRENCES))
            }
        }
        var lastMonth: Pair<Int, Int>? = null
        byDate.forEach { (date, dayOccurrences) ->
            val month = date.year to date.month.number
            if (month != lastMonth) {
                val first = lastMonth == null
                lastMonth = month
                item(key = "month-$month") {
                    Column(Modifier.padding(top = if (first) 0.dp else 16.dp)) {
                        Text(strings.monthTitle(date), style = MaterialTheme.typography.headlineSmall)
                        if (first) {
                            Text(
                                strings(StringKey.UPCOMING),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            item(key = "day-$date") {
                val weekday = strings(date.dayOfWeek.key())
                Text(
                    "${if (wide) weekday.take(3) else weekday} ${date.day}".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(dayOccurrences, key = { "${it.date}-${it.task.id}" }) { occurrence ->
                OccurrenceCard(occurrence.task, state.homies, myUserId)
            }
        }
    }

    if (wide) {
        Column(Modifier.fillMaxSize().widthIn(max = 1120.dp)) {
            LazyColumn(
                contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                userScrollEnabled = false,
                content = header,
            )
            Row(Modifier.weight(1f).padding(horizontal = horizontal), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                LazyColumn(
                    state = agendaState,
                    modifier = Modifier.weight(1.6f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = agenda,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MonthCard(byDate.keys, selectedDate, jumpTo)
                    HintText(strings(StringKey.CALENDAR_NOTE))
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            header()
            agenda()
            item { HintText(strings(StringKey.CALENDAR_NOTE), Modifier.padding(top = 8.dp)) }
        }
    }

    if (exporting) {
        ExportDialog(
            homies = state.homies,
            myUserId = myUserId,
            onExport = { scope, name -> viewModel.export(scope, name) },
            onDismiss = { exporting = false },
        )
    }
}

@Composable
private fun FilterRow(state: CalendarState, myUserId: String, viewModel: CalendarViewModel) {
    val strings = LocalStrings.current
    val options = listOf(
        CalendarFilter.All to strings(StringKey.FILTER_ALL),
        CalendarFilter.Me(myUserId) to strings(StringKey.FILTER_ME),
    ) + state.homies.filter { it.userId != myUserId }.map { CalendarFilter.Person(it.userId) to it.name } + listOf(
        CalendarFilter.Everyone to strings(StringKey.EVERYONE),
        CalendarFilter.Unassigned to strings(StringKey.UNASSIGNED),
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (filter, label) -> FilterPill(label, state.filter == filter) { viewModel.filterBy(filter) } }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.secondaryContainer else CreamCard)
            .border(1.dp, if (selected) colors.secondary else colors.outlineVariant, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.onSecondaryContainer)
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSecondaryContainer else colors.onSurface)
    }
}

@Composable
private fun OccurrenceCard(task: ChoreTask, homies: List<Homie>, myUserId: String) {
    val strings = LocalStrings.current
    val tone = toneOf(task.assignee, homies)
    val label = assigneeLabel(task.assignee, homies, strings).let {
        if (task.assignee.userIds == setOf(myUserId)) "$it · ${strings(StringKey.YOU)}" else it
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = CreamCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tone.content.copy(alpha = 0.55f)))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.what, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(timeRange(task), task.where.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SoftChip(label, tone, Modifier.padding(end = 16.dp).width(IntrinsicSize.Max), bordered = true)
        }
    }
}

@Composable
private fun MonthCard(busyDays: Set<LocalDate>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val strings = LocalStrings.current
    val today = remember { today() }
    var firstOfMonth by remember { mutableStateOf(LocalDate(today.year, today.month.number, 1)) }
    val daysInMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    val leading = firstOfMonth.dayOfWeek.isoDayNumber - 1
    val cells = List(leading) { null } + (1..daysInMonth).map { LocalDate(firstOfMonth.year, firstOfMonth.month.number, it) }

    SectionCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.monthName(firstOfMonth).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { firstOfMonth = firstOfMonth.minus(1, DateTimeUnit.MONTH) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = strings(StringKey.PREVIOUS_MONTH),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(onClick = { firstOfMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = strings(StringKey.NEXT_MONTH),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Row {
            DayOfWeek.entries.forEach { day ->
                Text(
                    strings(day.key()).take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row {
                week.forEach { date -> DayCell(date, DayState(date == today, date in busyDays, date == selected), onSelect, Modifier.weight(1f)) }
                repeat(DAYS_PER_WEEK - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private data class DayState(val isToday: Boolean, val isBusy: Boolean, val isSelected: Boolean)

@Composable
private fun DayCell(date: LocalDate?, state: DayState, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.height(52.dp).padding(2.dp), contentAlignment = Alignment.Center) {
        if (date == null) return@Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(max = 56.dp)
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    when {
                        state.isSelected -> colors.primaryContainer
                        state.isToday -> colors.secondaryContainer
                        else -> Color.Transparent
                    },
                )
                .then(if (state.isSelected) Modifier.border(1.5.dp, colors.primary, MaterialTheme.shapes.medium) else Modifier)
                .clickable(role = Role.Button) { onSelect(date) },
        ) {
            Text(
                date.day.toString(),
                style = if (state.isToday || state.isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = when {
                    state.isSelected -> colors.onPrimaryContainer
                    state.isToday -> colors.onSecondaryContainer
                    else -> colors.onSurface
                },
            )
            if (state.isBusy) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
            }
        }
    }
}

@Composable
private fun ExportDialog(
    homies: List<Homie>,
    myUserId: String,
    onExport: (ExportScope, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var single by remember { mutableStateOf(true) }
    var selectedId by remember { mutableStateOf(myUserId) }
    val selected = homies.firstOrNull { it.userId == selectedId }
    fun label(homie: Homie) =
        if (homie.userId == myUserId) strings(StringKey.MYSELF_NAMED, "name" to homie.name) else homie.name

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CreamCard,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(strings(StringKey.EXPORT_DIALOG_TITLE), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings(StringKey.EXPORT_FOR), style = MaterialTheme.typography.bodyMedium)
                ScopeOption(strings(StringKey.SCOPE_SINGLE), single) { single = true }
                SelectField(
                    label = null,
                    selected = selected?.let(::label).orEmpty(),
                    options = homies,
                    optionLabel = ::label,
                    onSelect = {
                        selectedId = it.userId
                        single = true
                    },
                    modifier = Modifier.padding(start = 48.dp),
                )
                ScopeOption(strings(StringKey.SCOPE_ALL), !single) { single = false }
            }
        },
        confirmButton = {
            PrimaryButton(strings(StringKey.EXPORT), {
                if (single) {
                    onExport(ExportScope.Single(selectedId), IcsExporter.fileName(selected?.name))
                } else {
                    onExport(ExportScope.All, IcsExporter.fileName(null))
                }
                onDismiss()
            })
        },
        dismissButton = { TertiaryButton(strings(StringKey.CANCEL), onDismiss) },
    )
}

@Composable
private fun ScopeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
