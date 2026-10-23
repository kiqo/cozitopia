package app.cozitopia.domain

import kotlin.uuid.Uuid
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

object TaskPlanner {
    private const val MINUTES_PER_HOUR = 60
    private const val DAYS_PER_WEEK = 7
    private const val MAX_SHIFTS = 4

    private class Interval(val start: Int, val end: Int)

    private class Day(var cursor: Int, val blocked: MutableList<Interval> = mutableListOf())

    private data class Placement(val date: LocalDate, val start: LocalTime, val exceedsDay: Boolean)

    data class ImportSummary(val imported: Int, val skipped: Int)

    data class ImportMerge(val tasks: List<ChoreTask>, val summary: ImportSummary)

    private data class DayKey(val start: LocalTime, val end: LocalTime, val date: LocalDate)

    fun plan(rows: List<CsvRow>, homies: List<Homie>, today: LocalDate): List<ChoreTask> =
        schedule(
            rows.map { row ->
                val resolution = ChecklistCsv.resolveAssignee(row.assigneeNames, homies)
                val recurrence = if (row.repetitionText.isBlank()) {
                    Schedule.propose(row.frequencyText, windowFor(resolution.assignee, homies).weekday)
                } else {
                    RecurrenceText.parse(row.repetitionText)
                }
                val start = ChecklistCsv.parseStart(row.nextStartText)
                val pinned = recurrence != null && start != null && !row.pinnedText.equals("false", ignoreCase = true)
                ChoreTask(
                    id = Uuid.random().toString(),
                    what = row.what,
                    where = row.where,
                    frequencyText = row.frequencyText,
                    durationMinutes = row.durationMinutes,
                    assignee = resolution.assignee,
                    recurrence = recurrence,
                    nextDate = start?.first.takeIf { pinned },
                    startTime = start?.second.takeIf { pinned },
                    pinned = pinned,
                    warnings = buildSet {
                        if (row.nextStartText.isNotBlank() && start == null) add(TaskWarning.INVALID_START)
                        if (resolution.unknownNames.isNotEmpty()) add(TaskWarning.UNKNOWN_ASSIGNEE)
                        if (!row.durationValid) add(TaskWarning.INVALID_DURATION)
                        if (recurrence == null) add(TaskWarning.UNRECOGNIZED_FREQUENCY)
                    },
                )
            },
            homies,
            today,
        )

    fun addNew(existing: List<ChoreTask>, imported: List<ChoreTask>): ImportMerge {
        val knownIds = existing.mapTo(mutableSetOf()) { it.id }
        val added = imported.filter { knownIds.add(it.id) }
        return ImportMerge(existing + added, ImportSummary(added.size, imported.size - added.size))
    }

    fun windowFor(assignee: Assignee, homies: List<Homie>): DayWindow =
        assignee.userIds.singleOrNull()
            ?.takeIf { assignee.kind == AssigneeKind.PEOPLE }
            ?.let { id -> homies.firstOrNull { it.userId == id }?.window }
            ?: DayWindow.DEFAULT

    fun withRecurrence(task: ChoreTask, recurrence: Recurrence): ChoreTask =
        task.copy(recurrence = recurrence, warnings = task.warnings - TaskWarning.UNRECOGNIZED_FREQUENCY)

    fun withFrequency(task: ChoreTask, frequencyText: String, homies: List<Homie>): ChoreTask {
        val recurrence = Schedule.propose(frequencyText, windowFor(task.assignee, homies).weekday)
        return task.copy(
            frequencyText = frequencyText,
            recurrence = recurrence,
            warnings = if (recurrence == null) {
                task.warnings + TaskWarning.UNRECOGNIZED_FREQUENCY
            } else {
                task.warnings - TaskWarning.UNRECOGNIZED_FREQUENCY
            },
        )
    }

    fun withAssignee(task: ChoreTask, assignee: Assignee, homies: List<Homie>): ChoreTask =
        moved(task, assignee, homies, homies).let { it.copy(warnings = it.warnings - TaskWarning.UNKNOWN_ASSIGNEE) }

    fun withHomies(tasks: List<ChoreTask>, before: List<Homie>, after: List<Homie>): List<ChoreTask> =
        tasks.map { moved(it, it.assignee, before, after) }

    fun assignEvenly(tasks: List<ChoreTask>, homies: List<Homie>): List<ChoreTask> {
        if (homies.isEmpty()) return tasks
        val load = homies.associate { homie ->
            homie.userId to tasks
                .filter { it.assignee.kind == AssigneeKind.PEOPLE && it.assignee.includes(homie.userId) }
                .sumOf(::yearlyHours)
        }.toMutableMap()
        val assigned = tasks
            .filter { it.assignee.kind == AssigneeKind.UNASSIGNED }
            .sortedByDescending(::yearlyHours)
            .associate { task ->
                val target = load.minBy { it.value }.key
                load[target] = load.getValue(target) + yearlyHours(task)
                task.id to Assignee.of(target)
            }
        return tasks.map { task -> assigned[task.id]?.let { withAssignee(task, it, homies) } ?: task }
    }

    fun yearlyHours(task: ChoreTask): Double =
        task.recurrence?.let { task.durationMinutes / MINUTES_PER_HOUR.toDouble() * Schedule.occurrencesPerYear(it) }
            ?: 0.0

    fun withStart(task: ChoreTask, date: LocalDate, time: LocalTime): ChoreTask =
        task.copy(nextDate = date, startTime = time, pinned = true, warnings = task.warnings - TaskWarning.INVALID_START)

    fun unpinned(task: ChoreTask): ChoreTask = task.copy(pinned = false)

    fun schedule(tasks: List<ChoreTask>, homies: List<Homie>, today: LocalDate): List<ChoreTask> {
        val days = mutableMapOf<DayKey, Day>()
        val pinned = tasks.filter(::isPinned)
        pinned.filter { it.durationMinutes > 0 }.forEach { task ->
            val window = windowFor(task.assignee, homies)
            val start = requireNotNull(task.startTime).minutes()
            days.getOrPut(DayKey(window.start, window.end, requireNotNull(task.nextDate))) {
                Day(window.start.minutes())
            }.blocked.add(Interval(start, start + task.durationMinutes))
        }
        val placements = tasks
            .filterNot(::isPinned)
            .mapNotNull { task -> task.recurrence?.let { Triple(task, it, Schedule.nextDate(it, today)) } }
            .sortedBy { it.third }
            .associate { (task, recurrence, base) ->
                task.id to place(task.durationMinutes, recurrence, base, windowFor(task.assignee, homies), days)
            }
        return tasks.map { task ->
            if (isPinned(task)) return@map task
            val placement = placements[task.id]
            task.copy(
                nextDate = placement?.date,
                startTime = placement?.start,
                pinned = false,
                warnings = if (placement?.exceedsDay == true) {
                    task.warnings + TaskWarning.EXCEEDS_DAY
                } else {
                    task.warnings - TaskWarning.EXCEEDS_DAY
                },
            )
        }
    }

    private fun isPinned(task: ChoreTask): Boolean =
        task.pinned && task.recurrence != null && task.nextDate != null && task.startTime != null

    private fun place(
        duration: Int,
        recurrence: Recurrence,
        base: LocalDate,
        window: DayWindow,
        days: MutableMap<DayKey, Day>,
    ): Placement {
        if (duration == 0) return Placement(base, window.start, false)
        val start = window.start.minutes()
        val end = window.end.minutes()
        val step = if (recurrence.weekday != null) DAYS_PER_WEEK else 1
        var date = base
        var shifts = 0
        while (true) {
            val day = days.getOrPut(DayKey(window.start, window.end, date)) { Day(start) }
            val slot = firstFit(day, duration, end)
            when {
                slot != null -> {
                    day.cursor = slot + duration
                    return Placement(date, slot.toTime(), false)
                }
                day.cursor == start && day.blocked.isEmpty() && start + duration > end -> {
                    day.cursor = end
                    return Placement(date, (end - duration).coerceAtLeast(0).toTime(), end - duration < 0)
                }
            }
            shifts += 1
            date = if (shifts <= MAX_SHIFTS) {
                date.plus(step, DateTimeUnit.DAY)
            } else {
                shifts = 0
                Schedule.nextDate(recurrence, date.plus(1, DateTimeUnit.DAY))
            }
        }
    }

    private fun firstFit(day: Day, duration: Int, end: Int): Int? {
        var slot = day.cursor
        while (true) {
            val hit = day.blocked.firstOrNull { it.start < slot + duration && slot < it.end } ?: break
            slot = hit.end
        }
        return slot.takeIf { it + duration <= end }
    }

    private fun moved(task: ChoreTask, assignee: Assignee, before: List<Homie>, after: List<Homie>): ChoreTask {
        val recurrence = task.recurrence
        val previous = windowFor(task.assignee, before).weekday
        val next = windowFor(assignee, after).weekday
        if (recurrence == null || recurrence.weekday != previous || previous == next) {
            return task.copy(assignee = assignee)
        }
        return task.copy(assignee = assignee, recurrence = recurrence.copy(weekday = next))
    }

    private fun LocalTime.minutes(): Int = hour * MINUTES_PER_HOUR + minute

    private fun Int.toTime(): LocalTime = LocalTime(this / MINUTES_PER_HOUR, this % MINUTES_PER_HOUR)
}
