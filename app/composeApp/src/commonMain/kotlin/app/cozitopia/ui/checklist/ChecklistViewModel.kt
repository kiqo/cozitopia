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
) {
    val canFinish: Boolean
        get() = tasks.none { it.recurrence == null || TaskWarning.INVALID_DURATION in it.warnings }
}

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
        mutableState.update {
            it.copy(
                homies = homies,
                tasks = TaskPlanner.schedule(TaskPlanner.withHomies(it.tasks, it.homies, homies), homies, today()),
            )
        }
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

    fun setWhat(taskId: String, what: String) = edit(taskId) { task, _ -> task.copy(what = what) }

    fun setWhere(taskId: String, where: String) = edit(taskId) { task, _ -> task.copy(where = where) }

    fun setDuration(taskId: String, text: String) = edit(taskId) { task, _ ->
        val minutes = ChecklistCsv.parseDuration(text)
        val warnings = if (minutes == null) {
            task.warnings + TaskWarning.INVALID_DURATION
        } else {
            task.warnings - TaskWarning.INVALID_DURATION
        }
        task.copy(durationMinutes = minutes ?: 0, warnings = warnings)
    }

    fun setAssignee(taskId: String, assignee: Assignee) =
        edit(taskId) { task, homies -> TaskPlanner.withAssignee(task, assignee, homies) }

    fun setFrequency(taskId: String, text: String) =
        edit(taskId) { task, homies -> TaskPlanner.withFrequency(task, text, homies) }

    fun setRecurrence(taskId: String, recurrence: Recurrence) =
        edit(taskId) { task, _ -> TaskPlanner.withRecurrence(task, recurrence) }

    fun delete(taskId: String) = mutableState.update { state ->
        state.copy(tasks = TaskPlanner.schedule(state.tasks.filterNot { it.id == taskId }, state.homies, today()))
    }

    fun assignAll() = mutableState.update {
        it.copy(tasks = TaskPlanner.schedule(TaskPlanner.assignEvenly(it.tasks, it.homies), it.homies, today()))
    }

    fun refreshDates() = mutableState.update {
        it.copy(tasks = TaskPlanner.schedule(it.tasks, it.homies, today()))
    }

    fun finish() = launchCatching {
        if (!mutableState.value.canFinish) return@launchCatching
        taskRepository.saveChecklist(homeId, mutableState.value.tasks)
        mutableState.update { it.copy(finished = true) }
    }

    fun dismissFinished() = mutableState.update { it.copy(finished = false) }

    private fun edit(taskId: String, transform: (ChoreTask, List<Homie>) -> ChoreTask) =
        mutableState.update { state ->
            val edited = state.tasks.map { if (it.id == taskId) transform(it, state.homies) else it }
            state.copy(tasks = TaskPlanner.schedule(edited, state.homies, today()))
        }

    private companion object {
        const val TEMPLATE_FILE_NAME = "cozitopia-template.csv"
        const val CSV_MIME_TYPE = "text/csv"
    }
}
