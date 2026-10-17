package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class TaskPlannerTest {
    private val today = LocalDate(2026, 10, 17)
    private val alice = Homie("u1", "Alice", Role.ADMIN)
    private val bob = Homie("u2", "Bob", Role.HOMIE)

    private fun task(id: String, minutes: Int, recurrence: Recurrence, assignee: Assignee = Assignee.Everyone) =
        ChoreTask(id, id, "", "", minutes, assignee, recurrence, today)

    private val yearly = Recurrence(RecurrenceUnit.YEAR)
    private val monthly = Recurrence(RecurrenceUnit.MONTH)

    @Test
    fun plansRowsWithWarnings() {
        val rows = ChecklistCsv.parse(
            "Ok,Here,1x Jahr (Herbst),2h,Alice\n" +
                "Odd,Here,sometimes,soon,Carol\n" +
                "Free,Here,1x Woche,1h,",
        ).rows

        val tasks = TaskPlanner.plan(rows, listOf(alice, bob), today)

        assertEquals(LocalDate(2027, 10, 2), tasks[0].nextDate)
        assertEquals(Assignee.of("u1"), tasks[0].assignee)
        assertTrue(tasks[0].warnings.isEmpty())
        assertEquals(
            setOf(TaskWarning.UNKNOWN_ASSIGNEE, TaskWarning.INVALID_DURATION, TaskWarning.UNRECOGNIZED_FREQUENCY),
            tasks[1].warnings,
        )
        assertEquals(Assignee.Unassigned, tasks[2].assignee)
    }

    @Test
    fun recomputesNextDateWhenRecurrenceChanges() {
        val planned = TaskPlanner.plan(ChecklistCsv.parse("A,B,sometimes,1h,").rows, listOf(alice), today).single()

        val updated = TaskPlanner.withRecurrence(planned, Recurrence(RecurrenceUnit.DAY, 1), today)

        assertNotEquals(planned.nextDate, updated.nextDate)
        assertEquals(today, updated.nextDate)
        assertTrue(TaskWarning.UNRECOGNIZED_FREQUENCY !in updated.warnings)
    }

    @Test
    fun refreshesDates() {
        val stale = task("a", 60, Recurrence(RecurrenceUnit.DAY, 1)).copy(nextDate = LocalDate(2020, 1, 1))

        val refreshed = TaskPlanner.refreshDates(listOf(stale), today)

        assertEquals(today, refreshed.single().nextDate)
    }

    @Test
    fun assignsEveryTaskGreedilyByYearlyLoad() {
        val tasks = listOf(
            task("small", 60, yearly),
            task("big", 60, monthly, Assignee.Unassigned),
            task("medium", 180, yearly),
            task("tiny", 120, yearly, Assignee.of("u2")),
        )

        val assigned = TaskPlanner.assignEvenly(tasks, listOf(alice, bob)).associate { it.id to it.assignee }

        assertEquals(Assignee.of("u1"), assigned["big"])
        assertEquals(Assignee.of("u2"), assigned["medium"])
        assertEquals(Assignee.of("u2"), assigned["tiny"])
        assertEquals(Assignee.of("u2"), assigned["small"])
    }

    @Test
    fun assignsAllTasksToSingleHomieAndClearsUnknownAssigneeWarning() {
        val tasks = listOf(
            task("a", 60, yearly).copy(warnings = setOf(TaskWarning.UNKNOWN_ASSIGNEE)),
            task("b", 60, monthly, Assignee.Unassigned),
        )

        val assigned = TaskPlanner.assignEvenly(tasks, listOf(alice))

        assertTrue(assigned.all { it.assignee == Assignee.of("u1") })
        assertTrue(assigned.all { it.warnings.isEmpty() })
    }

    @Test
    fun leavesTasksUnchangedWithoutHomies() {
        val tasks = listOf(task("a", 60, yearly))

        assertEquals(tasks, TaskPlanner.assignEvenly(tasks, emptyList()))
    }
}
