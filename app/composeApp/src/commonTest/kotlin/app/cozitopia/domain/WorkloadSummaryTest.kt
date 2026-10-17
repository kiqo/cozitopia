package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class WorkloadSummaryTest {
    private val alice = Homie("a", "Alice", Role.ADMIN)
    private val bob = Homie("b", "Bob", Role.HOMIE)

    private fun task(id: String, where: String, minutes: Int, assignee: Assignee, recurrence: Recurrence?) =
        ChoreTask(id, id, where, "", minutes, assignee, recurrence, LocalDate(2026, 10, 17))

    @Test
    fun sumsMinutesTasksAndRoomsPerHomie() {
        val tasks = listOf(
            task("1", "Kitchen", 60, Assignee.Everyone, Recurrence(RecurrenceUnit.MONTH)),
            task("2", "Bath", 30, Assignee.of("a", "b"), Recurrence(RecurrenceUnit.WEEK)),
            task("3", "-", 45, Assignee.of("a"), Recurrence(RecurrenceUnit.YEAR)),
            task("4", " Kitchen ", 20, Assignee.of("a"), null),
        )

        val result = WorkloadSummary.of(tasks, listOf(alice, bob))

        assertEquals(Workload(alice, 720 + 1560 + 45, 194, 4, listOf("Bath", "Kitchen")), result[0])
        assertEquals(Workload(bob, 720 + 1560, 190, 2, listOf("Bath", "Kitchen")), result[1])
    }

    @Test
    fun listsHomiesWithoutTasksAndAnUnassignedRow() {
        val tasks = listOf(task("1", "Hall", 10, Assignee.Unassigned, Recurrence(RecurrenceUnit.YEAR)))

        val result = WorkloadSummary.of(tasks, listOf(alice))

        assertEquals(listOf(Workload(alice, 0, 0, 0, emptyList()), Workload(null, 10, 1, 1, listOf("Hall"))), result)
    }
}
