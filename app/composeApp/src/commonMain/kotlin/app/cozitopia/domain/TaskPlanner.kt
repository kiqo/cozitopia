package app.cozitopia.domain

import kotlinx.datetime.LocalDate

object TaskPlanner {
    fun plan(rows: List<CsvRow>, homies: List<Homie>, today: LocalDate): List<ChoreTask> =
        rows.mapIndexed { index, row ->
            val recurrence = Schedule.propose(row.frequencyText)
            ChoreTask(
                id = "task-$index",
                what = row.what,
                where = row.where,
                frequencyText = row.frequencyText,
                durationMinutes = row.durationMinutes,
                assignee = ChecklistCsv.resolveAssignee(row.assigneeNames, homies),
                recurrence = recurrence,
                nextDate = Schedule.nextDate(recurrence, today),
            )
        }

    fun withRecurrence(task: ChoreTask, recurrence: Recurrence, today: LocalDate): ChoreTask =
        task.copy(recurrence = recurrence, nextDate = Schedule.nextDate(recurrence, today))

    fun refreshDates(tasks: List<ChoreTask>, today: LocalDate): List<ChoreTask> =
        tasks.map { it.copy(nextDate = Schedule.nextDate(it.recurrence, today)) }

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
        return tasks.map { it.copy(assignee = assigned.getValue(it.id)) }
    }

    fun yearlyHours(task: ChoreTask): Double =
        task.durationMinutes / 60.0 * Schedule.occurrencesPerYear(task.recurrence)
}
