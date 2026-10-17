package app.cozitopia.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

object TaskPlanner {
    private const val MINUTES_PER_HOUR = 60

    private class Day(var cursor: Int, var alone: Boolean)

    private data class Placement(val date: LocalDate, val start: LocalTime, val exceedsDay: Boolean)

    private data class DayKey(val start: LocalTime, val end: LocalTime, val date: LocalDate)

    fun plan(rows: List<CsvRow>, homies: List<Homie>, today: LocalDate): List<ChoreTask> =
        schedule(
            rows.mapIndexed { index, row ->
                val resolution = ChecklistCsv.resolveAssignee(row.assigneeNames, homies)
                val recurrence = Schedule.propose(row.frequencyText, windowFor(resolution.assignee, homies).weekday)
                ChoreTask(
                    id = "task-$index",
                    what = row.what,
                    where = row.where,
                    frequencyText = row.frequencyText,
                    durationMinutes = row.durationMinutes,
                    assignee = resolution.assignee,
                    recurrence = recurrence,
                    nextDate = null,
                    warnings = buildSet {
                        if (resolution.unknownNames.isNotEmpty()) add(TaskWarning.UNKNOWN_ASSIGNEE)
                        if (!row.durationValid) add(TaskWarning.INVALID_DURATION)
                        if (recurrence == null) add(TaskWarning.UNRECOGNIZED_FREQUENCY)
                    },
                )
            },
            homies,
            today,
        )

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
        val load = homies.associate { it.userId to 0.0 }.toMutableMap()
        val assigned = tasks
            .sortedByDescending(::yearlyHours)
            .associate { task ->
                val target = load.minBy { it.value }.key
                load[target] = load.getValue(target) + yearlyHours(task)
                task.id to Assignee.of(target)
            }
        return tasks.map { withAssignee(it, assigned.getValue(it.id), homies) }
    }

    fun yearlyHours(task: ChoreTask): Double =
        task.recurrence?.let { task.durationMinutes / MINUTES_PER_HOUR.toDouble() * Schedule.occurrencesPerYear(it) }
            ?: 0.0

    fun schedule(tasks: List<ChoreTask>, homies: List<Homie>, today: LocalDate): List<ChoreTask> {
        val days = mutableMapOf<DayKey, Day>()
        val placements = tasks
            .mapNotNull { task -> task.recurrence?.let { Triple(task, it, Schedule.nextDate(it, today)) } }
            .sortedBy { it.third }
            .associate { (task, recurrence, base) ->
                task.id to place(task.durationMinutes, recurrence, base, windowFor(task.assignee, homies), days)
            }
        return tasks.map { task ->
            val placement = placements[task.id]
            task.copy(
                nextDate = placement?.date,
                startTime = placement?.start,
                warnings = if (placement?.exceedsDay == true) {
                    task.warnings + TaskWarning.EXCEEDS_DAY
                } else {
                    task.warnings - TaskWarning.EXCEEDS_DAY
                },
            )
        }
    }

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
        var date = base
        while (true) {
            val key = DayKey(window.start, window.end, date)
            val day = days[key]
            when {
                day == null && start + duration <= end -> {
                    days[key] = Day(start + duration, false)
                    return Placement(date, window.start, false)
                }
                day == null -> {
                    days[key] = Day(end, true)
                    return Placement(date, (end - duration).coerceAtLeast(0).toTime(), end - duration < 0)
                }
                !day.alone && day.cursor + duration <= end -> {
                    val placed = Placement(date, day.cursor.toTime(), false)
                    day.cursor += duration
                    return placed
                }
            }
            date = Schedule.nextDate(recurrence, date.plus(1, DateTimeUnit.DAY))
        }
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
