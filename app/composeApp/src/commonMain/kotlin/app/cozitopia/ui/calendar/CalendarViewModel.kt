package app.cozitopia.ui.calendar

import app.cozitopia.data.ErrorBus
import app.cozitopia.data.HomeRepository
import app.cozitopia.data.TaskRepository
import app.cozitopia.data.exportFile
import app.cozitopia.domain.AssigneeKind
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.ExportScope
import app.cozitopia.domain.Homie
import app.cozitopia.domain.IcsExporter
import app.cozitopia.domain.Schedule
import app.cozitopia.ui.ScreenViewModel
import app.cozitopia.ui.today
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

sealed interface CalendarFilter {
    fun matches(task: ChoreTask): Boolean

    data object All : CalendarFilter {
        override fun matches(task: ChoreTask) = true
    }

    data class Me(val userId: String) : CalendarFilter {
        override fun matches(task: ChoreTask) = IcsExporter.isOwn(task, userId)
    }

    data class Person(val userId: String) : CalendarFilter {
        override fun matches(task: ChoreTask) =
            task.assignee.kind == AssigneeKind.PEOPLE && task.assignee.includes(userId)
    }

    data object Everyone : CalendarFilter {
        override fun matches(task: ChoreTask) = task.assignee.kind == AssigneeKind.EVERYONE
    }

    data object Unassigned : CalendarFilter {
        override fun matches(task: ChoreTask) = task.assignee.kind == AssigneeKind.UNASSIGNED
    }
}

data class Occurrence(val date: LocalDate, val task: ChoreTask)

data class CalendarState(
    val tasks: List<ChoreTask> = emptyList(),
    val homies: List<Homie> = emptyList(),
    val filter: CalendarFilter = CalendarFilter.All,
) {
    val occurrences: List<Occurrence>
        get() {
            val from = today()
            val until = from.plus(HORIZON_MONTHS, DateTimeUnit.MONTH)
            return tasks.filter(filter::matches)
                .flatMap { task ->
                    task.recurrence?.let { Schedule.occurrences(it, from, until) }.orEmpty().map { Occurrence(it, task) }
                }
                .sortedBy { it.date }
        }

    private companion object {
        const val HORIZON_MONTHS = 12
    }
}

class CalendarViewModel(
    private val homeId: String,
    private val homeName: String,
    private val homes: HomeRepository,
    private val taskRepository: TaskRepository,
    errors: ErrorBus,
) : ScreenViewModel(errors) {
    private val mutableState = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = mutableState

    fun refresh() = launchCatching {
        val tasks = taskRepository.tasks(homeId).filter(ChoreTask::isValid)
        val homies = homes.members(homeId).map { it.homie }
        mutableState.update { it.copy(tasks = tasks, homies = homies) }
    }

    fun filterBy(filter: CalendarFilter) = mutableState.update { it.copy(filter = filter) }

    fun export(scope: ExportScope, fileName: String) = launchCatching {
        val state = mutableState.value
        val tasks = IcsExporter.filter(state.tasks, scope)
        exportFile(fileName, ICS_MIME_TYPE, IcsExporter.export(tasks, homeName, state.homies, Clock.System.now()))
    }

    private companion object {
        const val ICS_MIME_TYPE = "text/calendar"
    }
}
