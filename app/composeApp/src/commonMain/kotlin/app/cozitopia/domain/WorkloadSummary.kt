package app.cozitopia.domain

import kotlin.math.roundToInt

data class Workload(
    val homie: Homie?,
    val yearlyMinutes: Int,
    val monthlyMinutes: Int,
    val taskCount: Int,
    val rooms: List<String>,
)

object WorkloadSummary {
    private const val NO_ROOM = "-"
    private const val MONTHS_PER_YEAR = 12

    fun of(tasks: List<ChoreTask>, homies: List<Homie>): List<Workload> {
        val unassigned = tasks.filter { it.assignee.kind == AssigneeKind.UNASSIGNED }
        return homies.map { homie -> workload(homie, tasks.filter { it.assignee.includes(homie.userId) }) } +
            listOfNotNull(unassigned.takeIf { it.isNotEmpty() }?.let { workload(null, it) })
    }

    private fun workload(homie: Homie?, tasks: List<ChoreTask>): Workload {
        val yearly = tasks.sumOf { yearlyMinutes(it) }
        return Workload(
            homie = homie,
            yearlyMinutes = yearly.roundToInt(),
            monthlyMinutes = (yearly / MONTHS_PER_YEAR).roundToInt(),
            taskCount = tasks.size,
            rooms = tasks.map { it.where.trim() }.filter { it.isNotEmpty() && it != NO_ROOM }.distinct().sorted(),
        )
    }

    private fun yearlyMinutes(task: ChoreTask): Double =
        task.recurrence?.let { task.durationMinutes * Schedule.occurrencesPerYear(it) } ?: 0.0
}
