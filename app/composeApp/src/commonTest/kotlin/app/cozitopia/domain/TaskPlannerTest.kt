package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

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

        assertEquals(LocalDate(2027, 10, 3), tasks[0].nextDate)
        assertEquals(Assignee.of("u1"), tasks[0].assignee)
        assertTrue(tasks[0].warnings.isEmpty())
        assertEquals(
            setOf(TaskWarning.UNKNOWN_ASSIGNEE, TaskWarning.INVALID_DURATION, TaskWarning.UNRECOGNIZED_FREQUENCY),
            tasks[1].warnings,
        )
        assertEquals(Assignee.Unassigned, tasks[2].assignee)
        assertNull(tasks[1].recurrence)
        assertNull(tasks[1].nextDate)
    }

    @Test
    fun proposesTheWeekdayOfASingleAssigneeAndSundayOtherwise() {
        val monday = alice.copy(window = DayWindow(DayOfWeek.MONDAY))
        val rows = ChecklistCsv.parse(
            "Solo,Here,1x Woche,1h,Alice\n" +
                "Duo,Here,1x Woche,1h,Alice;Bob\n" +
                "All,Here,1x Woche,1h,Everyone\n" +
                "Nobody,Here,1x Woche,1h,",
        ).rows

        val weekdays = TaskPlanner.plan(rows, listOf(monday, bob), today).map { it.recurrence?.weekday }

        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SUNDAY, DayOfWeek.SUNDAY),
            weekdays,
        )
    }

    @Test
    fun followsTheAssigneeAndTheirChangedWeekday() {
        val weekly = Recurrence(RecurrenceUnit.WEEK, 1, DayOfWeek.SUNDAY)
        val monday = alice.copy(window = DayWindow(DayOfWeek.MONDAY))
        val friday = alice.copy(window = DayWindow(DayOfWeek.FRIDAY))
        val unassigned = task("a", 60, weekly, Assignee.Unassigned)

        val assigned = TaskPlanner.withAssignee(unassigned, Assignee.of("u1"), listOf(monday, bob))
        val changed = TaskPlanner.withHomies(listOf(assigned), listOf(monday, bob), listOf(friday, bob)).single()

        assertEquals(DayOfWeek.MONDAY, assigned.recurrence?.weekday)
        assertEquals(DayOfWeek.FRIDAY, changed.recurrence?.weekday)
        assertEquals(LocalDate(2026, 10, 23), TaskPlanner.schedule(listOf(changed), listOf(friday, bob), today).single().nextDate)
    }

    @Test
    fun keepsManuallyChosenWeekdayWhenAssigneeChanges() {
        val manual = task("a", 60, Recurrence(RecurrenceUnit.WEEK, 1, DayOfWeek.WEDNESDAY))
        val monday = alice.copy(window = DayWindow(DayOfWeek.MONDAY))

        val assigned = TaskPlanner.withAssignee(manual, Assignee.of("u1"), listOf(monday))

        assertEquals(DayOfWeek.WEDNESDAY, assigned.recurrence?.weekday)
    }

    @Test
    fun reparsesEditedFrequencyText() {
        val planned = TaskPlanner.plan(ChecklistCsv.parse("X,Y,1x Jahr,1h,\nA,B,1x Woche,1h,").rows, listOf(alice), today)[1]

        val broken = TaskPlanner.withFrequency(planned, "Alle 2-3 Tage", listOf(alice))
        val fixed = TaskPlanner.withFrequency(broken, "Alle 2 Monate", listOf(alice))

        assertNull(broken.recurrence)
        assertTrue(TaskWarning.UNRECOGNIZED_FREQUENCY in broken.warnings)
        assertEquals(RecurrenceUnit.MONTH, fixed.recurrence?.unit)
        assertEquals("Alle 2 Monate", fixed.frequencyText)
        assertTrue(TaskWarning.UNRECOGNIZED_FREQUENCY !in fixed.warnings)
        assertNull(TaskPlanner.schedule(listOf(broken), listOf(alice), today).single().nextDate)
    }

    @Test
    fun treatsUnfilledTasksAsZeroHoursWhenAssigning() {
        val unfilled = task("a", 60, yearly, Assignee.Unassigned).copy(recurrence = null, nextDate = null)

        val assigned = TaskPlanner.assignEvenly(
            listOf(unfilled, task("b", 60, monthly, Assignee.Unassigned)),
            listOf(alice, bob),
        )

        assertEquals(0.0, TaskPlanner.yearlyHours(unfilled))
        assertNull(assigned.first().recurrence)
        assertTrue(assigned.all { it.assignee.kind == AssigneeKind.PEOPLE })
    }

    private val daily = Recurrence(RecurrenceUnit.DAY, 1)

    private fun scheduled(vararg minutes: Int, window: DayWindow = DayWindow.DEFAULT): List<ChoreTask> {
        val homie = alice.copy(window = window)
        val tasks = minutes.mapIndexed { index, value -> task("t$index", value, daily, Assignee.of("u1")) }

        return TaskPlanner.schedule(tasks, listOf(homie), today)
    }

    @Test
    fun packsShortTasksBackToBackFromPreferredStart() {
        val tasks = scheduled(60, 120)

        assertEquals(listOf(LocalTime(10, 0), LocalTime(11, 0)), tasks.map { it.startTime })
        assertEquals(setOf(today), tasks.map { it.nextDate }.toSet())
    }

    @Test
    fun movesOverflowingTaskToNextOccurrence() {
        val tasks = scheduled(300, 240)

        assertEquals(listOf(today, today.plus(1, DateTimeUnit.DAY)), tasks.map { it.nextDate })
        assertEquals(listOf(LocalTime(10, 0), LocalTime(10, 0)), tasks.map { it.startTime })
    }

    @Test
    fun shiftsOverflowingYearlyTaskByOneWeekInsteadOfOneYear() {
        val yearly = Recurrence(RecurrenceUnit.YEAR, 1, DayOfWeek.SUNDAY, Ordinal.FIRST, month = 4)
        val tasks = listOf(task("a", 450, yearly, Assignee.of("u1")), task("b", 450, yearly, Assignee.of("u1")))

        val planned = TaskPlanner.schedule(tasks, listOf(alice), today)

        assertEquals(listOf(LocalDate(2027, 4, 4), LocalDate(2027, 4, 11)), planned.map { it.nextDate })
    }

    @Test
    fun startsLongTaskEarlierAndAlone() {
        val window = DayWindow(DayOfWeek.TUESDAY, LocalTime(17, 0), LocalTime(22, 0))

        val tasks = scheduled(360, 30, window = window)

        assertEquals(LocalTime(16, 0), tasks[0].startTime)
        assertEquals(today, tasks[0].nextDate)
        assertEquals(today.plus(1, DateTimeUnit.DAY), tasks[1].nextDate)
        assertEquals(LocalTime(17, 0), tasks[1].startTime)
    }

    @Test
    fun flagsTaskLongerThanADay() {
        val tasks = scheduled(1500)

        assertEquals(LocalTime(0, 0), tasks.single().startTime)
        assertTrue(TaskWarning.EXCEEDS_DAY in tasks.single().warnings)
    }

    @Test
    fun assignsOnlyUnassignedTasksBalancedOnTopOfExistingLoad() {
        val tasks = listOf(
            task("mine", 60, yearly, Assignee.of("u1")),
            task("shared", 60, monthly, Assignee.of("u1", "u2")),
            task("all", 600, yearly),
            task("open", 60, yearly, Assignee.Unassigned),
        )

        val assigned = TaskPlanner.assignEvenly(tasks, listOf(alice, bob)).associate { it.id to it.assignee }

        assertEquals(Assignee.of("u1"), assigned["mine"])
        assertEquals(Assignee.of("u1", "u2"), assigned["shared"])
        assertEquals(Assignee.Everyone, assigned["all"])
        assertEquals(Assignee.of("u2"), assigned["open"])
    }

    @Test
    fun balancesSeveralUnassignedTasksGreedily() {
        val tasks = listOf(
            task("small", 60, yearly, Assignee.Unassigned),
            task("big", 60, monthly, Assignee.Unassigned),
            task("medium", 180, yearly, Assignee.Unassigned),
        )

        val assigned = TaskPlanner.assignEvenly(tasks, listOf(alice, bob)).associate { it.id to it.assignee }

        assertEquals(Assignee.of("u1"), assigned["big"])
        assertEquals(Assignee.of("u2"), assigned["medium"])
        assertEquals(Assignee.of("u2"), assigned["small"])
    }

    @Test
    fun clearsUnknownAssigneeWarningWhenAssigning() {
        val tasks = listOf(task("a", 60, yearly, Assignee.Unassigned).copy(warnings = setOf(TaskWarning.UNKNOWN_ASSIGNEE)))

        val assigned = TaskPlanner.assignEvenly(tasks, listOf(alice))

        assertEquals(Assignee.of("u1"), assigned.single().assignee)
        assertTrue(assigned.single().warnings.isEmpty())
    }

    @Test
    fun keepsPinnedTasksAndPacksOthersAroundThem() {
        val pinned = task("pin", 60, daily, Assignee.of("u1"))
            .copy(nextDate = today, startTime = LocalTime(12, 0), pinned = true)
        val tasks = listOf(pinned, task("a", 120, daily, Assignee.of("u1")), task("b", 60, daily, Assignee.of("u1")))

        val planned = TaskPlanner.schedule(tasks, listOf(alice), today)

        assertEquals(LocalTime(12, 0), planned[0].startTime)
        assertEquals(today, planned[0].nextDate)
        assertTrue(planned[0].pinned)
        assertEquals(listOf(LocalTime(10, 0), LocalTime(13, 0)), planned.drop(1).map { it.startTime })
        assertEquals(setOf(today), planned.map { it.nextDate }.toSet())
    }

    @Test
    fun returnsUnpinnedTaskToComputedPlacement() {
        val pinned = task("pin", 60, daily, Assignee.of("u1"))
            .copy(nextDate = LocalDate(2027, 1, 5), startTime = LocalTime(15, 30), pinned = true)

        val planned = TaskPlanner.schedule(listOf(TaskPlanner.unpinned(pinned)), listOf(alice), today).single()

        assertEquals(today, planned.nextDate)
        assertEquals(LocalTime(10, 0), planned.startTime)
    }

    @Test
    fun leavesTasksUnchangedWithoutHomies() {
        val tasks = listOf(task("a", 60, yearly))

        assertEquals(tasks, TaskPlanner.assignEvenly(tasks, emptyList()))
    }
}
