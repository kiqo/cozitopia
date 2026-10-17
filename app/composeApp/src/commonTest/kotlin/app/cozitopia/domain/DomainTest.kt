package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class DomainTest {
    private val alice = Homie("a", "Alice", Role.ADMIN)
    private val bob = Homie("b", "Bob", Role.HOMIE)
    private val today = LocalDate(2026, 10, 17)

    @Test
    fun parsesGermanRowsAndAssignees() {
        val csv = "What,Where,Frequency,Duration,Assignee\n" +
            "'Steckdosen reinigen','Alle Räume','1x Jahr','3h'\n" +
            "Terrasse reinigen,Garten,1x Jahr (Herbst),2h,Alice;Bob\n"

        val rows = ChecklistCsv.parse(csv)

        assertEquals(2, rows.size)
        assertEquals("Alle Räume", rows[0].where)
        assertEquals(180, rows[0].durationMinutes)
        assertEquals(Assignee.Unassigned, ChecklistCsv.resolveAssignee(rows[0].assigneeNames, listOf(alice, bob)))
        assertEquals(Assignee.of("a", "b"), ChecklistCsv.resolveAssignee(rows[1].assigneeNames, listOf(alice, bob)))
    }

    @Test
    fun parsesSemicolonDelimitedWithoutHeader() {
        val rows = ChecklistCsv.parse("Fenster putzen;Haus;2x Jahr;90min")

        assertEquals(90, rows.single().durationMinutes)
    }

    @Test
    fun proposesYearlyAutumnRecurrence() {
        val recurrence = Schedule.propose("1x Jahr (Herbst)")

        assertEquals(RecurrenceUnit.YEAR, recurrence.unit)
        assertEquals(10, recurrence.month)
        assertEquals(LocalDate(2026, 10, 17), Schedule.nextDate(recurrence, today))
    }

    @Test
    fun twicePerYearBecomesSixMonthInterval() {
        val recurrence = Schedule.propose("2x Jahr")

        assertEquals(RecurrenceUnit.MONTH, recurrence.unit)
        assertEquals(6, recurrence.interval)
    }

    @Test
    fun monthlyFirstSaturdayAndLastFriday() {
        val first = Recurrence(RecurrenceUnit.MONTH, 1, DayOfWeek.SATURDAY, Ordinal.FIRST)
        val last = Recurrence(RecurrenceUnit.MONTH, 1, DayOfWeek.FRIDAY, Ordinal.LAST)

        assertEquals(LocalDate(2026, 11, 7), Schedule.nextDate(first, today))
        assertEquals(LocalDate(2026, 10, 30), Schedule.nextDate(last, today))
        assertEquals("FREQ=MONTHLY;INTERVAL=1;BYDAY=-1FR", Schedule.rrule(last))
    }

    @Test
    fun assignsEvenlyByHours() {
        val rows = ChecklistCsv.parse("a,x,1x Jahr,4h\nb,x,1x Jahr,2h\nc,x,1x Jahr,2h")
        val tasks = TaskPlanner.plan(rows, listOf(alice, bob), today)

        val assigned = TaskPlanner.assignEvenly(tasks, listOf(alice, bob))

        val hours = listOf(alice, bob).map { homie ->
            assigned.filter { it.assignee.includes(homie.userId) }.sumOf(TaskPlanner::yearlyHours)
        }
        assertEquals(listOf(4.0, 4.0), hours)
    }

    @Test
    fun exportFilterIncludesUnassignedAndOwn() {
        val tasks = TaskPlanner.plan(ChecklistCsv.parse("a,x,1x Jahr,1h\nb,x,1x Jahr,1h\nc,x,1x Jahr,1h"), listOf(alice, bob), today)
            .let { listOf(it[0].copy(assignee = Assignee.of("a")), it[1].copy(assignee = Assignee.of("b")), it[2]) }

        assertEquals(listOf("a", "c"), IcsExporter.filter(tasks, "a").map { it.what })
        assertEquals(3, IcsExporter.filter(tasks, "a", setOf("b")).size)
        assertEquals(3, IcsExporter.filter(tasks, "a", includeAll = true).size)
    }

    @Test
    fun icsContainsRruleAndCrlf() {
        val task = TaskPlanner.plan(ChecklistCsv.parse("Terrasse,Garten,1x Jahr (Herbst),2h"), listOf(alice), today).single()

        val ics = IcsExporter.export(listOf(task), "MyHome")

        assertTrue(ics.contains("RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=10;BYDAY=1SA\r\n"))
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261017"))
    }
}
