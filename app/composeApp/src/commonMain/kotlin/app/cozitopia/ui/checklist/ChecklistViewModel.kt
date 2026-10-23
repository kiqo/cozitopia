package app.cozitopia.ui.checklist

import app.cozitopia.data.AppError
import app.cozitopia.data.ErrorBus
import app.cozitopia.data.HomeRepository
import app.cozitopia.data.TaskRepository
import app.cozitopia.data.exportFile
import app.cozitopia.domain.Assignee
import app.cozitopia.domain.AssigneeKind
import app.cozitopia.domain.ChecklistCsv
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.Homie
import app.cozitopia.domain.IcsImporter
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.TaskPlanner
import app.cozitopia.domain.TaskWarning
import app.cozitopia.i18n.StringKey
import app.cozitopia.ui.ScreenViewModel
import app.cozitopia.ui.today
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class ChecklistState(
    val homies: List<Homie> = emptyList(),
    val tasks: List<ChoreTask> = emptyList(),
    val skippedLines: List<Int> = emptyList(),
    val synced: Boolean = false,
    val loaded: Boolean = false,
    val importSummary: TaskPlanner.ImportSummary? = null,
) {
    val assignBlocker: StringKey?
        get() = when {
            tasks.isEmpty() -> StringKey.ERROR_NO_TASKS
            homies.isEmpty() -> StringKey.ERROR_NO_HOMIES
            tasks.none { it.assignee.kind == AssigneeKind.UNASSIGNED } -> StringKey.ERROR_NO_UNASSIGNED
            else -> null
        }

    val canSync: Boolean
        get() = tasks.all(ChoreTask::isValid)
}

class ChecklistViewModel(
    private val homeId: String,
    private val homes: HomeRepository,
    private val taskRepository: TaskRepository,
    errors: ErrorBus,
) : ScreenViewModel(errors) {
    private val mutableState = MutableStateFlow(ChecklistState())
    val state: StateFlow<ChecklistState> = mutableState

    private val saveLock = Mutex()
    private var autosave: Job? = null
    private var unsaved = false

    fun refresh() = launchCatching {
        flushUnsaved()
        val homies = homes.members(homeId).map { it.homie }
        val saved = taskRepository.tasks(homeId)
        mutableState.update {
            it.copy(homies = homies, tasks = TaskPlanner.schedule(saved, homies, today()), loaded = true)
        }
    }

    fun downloadTemplate() = launchCatching {
        exportFile(TEMPLATE_FILE_NAME, CSV_MIME_TYPE, ChecklistCsv.TEMPLATE)
    }

    fun exportChecklist() = launchCatching {
        val state = mutableState.value
        exportFile(EXPORT_FILE_NAME, CSV_MIME_TYPE, ChecklistCsv.export(state.tasks, state.homies))
    }

    fun importFile(fileName: String, readText: suspend () -> String) = launchCatching {
        val text = readText()
        val homies = mutableState.value.homies
        val skippedLines: List<Int>
        val imported = if (fileName.endsWith(ICS_EXTENSION, ignoreCase = true)) {
            skippedLines = emptyList()
            IcsImporter.parse(text, homies)
        } else {
            val parsed = ChecklistCsv.parse(text)
            skippedLines = parsed.skippedLines
            TaskPlanner.plan(parsed.rows, homies, today())
        }
        change {
            val merge = TaskPlanner.addNew(it.tasks, imported)
            it.copy(
                tasks = TaskPlanner.schedule(merge.tasks, it.homies, today()),
                skippedLines = skippedLines,
                importSummary = merge.summary,
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

    fun setStart(taskId: String, date: LocalDate, time: LocalTime) =
        edit(taskId) { task, _ -> if (task.recurrence == null) task else TaskPlanner.withStart(task, date, time) }

    fun unpin(taskId: String) = edit(taskId) { task, _ -> TaskPlanner.unpinned(task) }

    fun delete(taskId: String) = changeTasks { state -> state.tasks.filterNot { it.id == taskId } }

    fun assignAll() {
        val blocker = mutableState.value.assignBlocker
        if (blocker != null) {
            errors.report(AppError.Known(blocker))
            return
        }
        changeTasks { TaskPlanner.assignEvenly(it.tasks, it.homies) }
    }

    fun refreshDates() = changeTasks { it.tasks }

    fun sync() = launchCatching {
        if (!mutableState.value.canSync) return@launchCatching
        flushUnsaved()
        mutableState.update { it.copy(synced = true) }
    }

    fun dismissSynced() = mutableState.update { it.copy(synced = false) }

    private fun edit(taskId: String, transform: (ChoreTask, List<Homie>) -> ChoreTask) =
        changeTasks { state -> state.tasks.map { if (it.id == taskId) transform(it, state.homies) else it } }

    private fun changeTasks(transform: (ChecklistState) -> List<ChoreTask>) =
        change { it.copy(tasks = TaskPlanner.schedule(transform(it), it.homies, today())) }

    private fun change(transform: (ChecklistState) -> ChecklistState) {
        mutableState.update(transform)
        unsaved = true
        autosave?.cancel()
        autosave = launchCatching {
            delay(AUTOSAVE_DELAY_MS)
            withContext(NonCancellable) { persist() }
        }
    }

    private suspend fun flushUnsaved() {
        autosave?.cancel()
        persist()
    }

    private suspend fun persist() = saveLock.withLock {
        if (!unsaved) return@withLock
        unsaved = false
        try {
            taskRepository.saveChecklist(homeId, mutableState.value.tasks)
        } catch (failure: Exception) {
            unsaved = true
            throw failure
        }
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 500L
        const val ICS_EXTENSION = ".ics"
        const val EXPORT_FILE_NAME = "happy-home-checklist.csv"
        const val TEMPLATE_FILE_NAME = "cozitopia-template.csv"
        const val CSV_MIME_TYPE = "text/csv"
    }
}
