package app.cozitopia.ui.checklist

import app.cozitopia.data.ErrorBus
import app.cozitopia.data.HomeRepository
import app.cozitopia.data.TaskRepository
import app.cozitopia.data.exportFile
import app.cozitopia.domain.Assignee
import app.cozitopia.domain.ChecklistCsv
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.Homie
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.TaskPlanner
import app.cozitopia.domain.TaskWarning
import app.cozitopia.ui.ScreenViewModel
import app.cozitopia.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ChecklistState(
    val homies: List<Homie> = emptyList(),
    val tasks: List<ChoreTask> = emptyList(),
    val skippedLines: List<Int> = emptyList(),
    val finished: Boolean = false,
    val importCount: Int = 0,
)

class ChecklistViewModel(
    private val homeId: String,
    private val homes: HomeRepository,
    private val taskRepository: TaskRepository,
    errors: ErrorBus,
) : ScreenViewModel(errors) {
    private val mutableState = MutableStateFlow(ChecklistState())
    val state: StateFlow<ChecklistState> = mutableState

    fun refresh() = launchCatching {
        val homies = homes.members(homeId).map { it.homie }
        mutableState.update { it.copy(homies = homies) }
    }

    fun downloadTemplate() = launchCatching {
        exportFile(TEMPLATE_FILE_NAME, CSV_MIME_TYPE, ChecklistCsv.TEMPLATE)
    }

    fun importCsv(readText: suspend () -> String) = launchCatching {
        val parsed = ChecklistCsv.parse(readText())
        mutableState.update {
            it.copy(
                tasks = TaskPlanner.plan(parsed.rows, it.homies, today()),
                skippedLines = parsed.skippedLines,
                importCount = it.importCount + 1,
            )
        }
    }

    fun setWhat(taskId: String, what: String) = edit(taskId) { it.copy(what = what) }

    fun setWhere(taskId: String, where: String) = edit(taskId) { it.copy(where = where) }

    fun setDuration(taskId: String, text: String) = edit(taskId) {
        val minutes = ChecklistCsv.parseDuration(text)
        val warnings = if (minutes == null) {
            it.warnings + TaskWarning.INVALID_DURATION
        } else {
            it.warnings - TaskWarning.INVALID_DURATION
        }
        it.copy(durationMinutes = minutes ?: 0, warnings = warnings)
    }

    fun setAssignee(taskId: String, assignee: Assignee) = edit(taskId) {
        it.copy(assignee = assignee, warnings = it.warnings - TaskWarning.UNKNOWN_ASSIGNEE)
    }

    fun setRecurrence(taskId: String, recurrence: Recurrence) = edit(taskId) {
        TaskPlanner.withRecurrence(it, recurrence, today())
    }

    fun delete(taskId: String) = mutableState.update { state ->
        state.copy(tasks = state.tasks.filterNot { it.id == taskId })
    }

    fun assignAll() = mutableState.update { it.copy(tasks = TaskPlanner.assignEvenly(it.tasks, it.homies)) }

    fun refreshDates() = mutableState.update { it.copy(tasks = TaskPlanner.refreshDates(it.tasks, today())) }

    fun finish() = launchCatching {
        taskRepository.saveChecklist(homeId, mutableState.value.tasks)
        mutableState.update { it.copy(finished = true) }
    }

    fun dismissFinished() = mutableState.update { it.copy(finished = false) }

    private fun edit(taskId: String, transform: (ChoreTask) -> ChoreTask) = mutableState.update { state ->
        state.copy(tasks = state.tasks.map { if (it.id == taskId) transform(it) else it })
    }

    private companion object {
        const val TEMPLATE_FILE_NAME = "cozitopia-template.csv"
        const val CSV_MIME_TYPE = "text/csv"
    }
}
