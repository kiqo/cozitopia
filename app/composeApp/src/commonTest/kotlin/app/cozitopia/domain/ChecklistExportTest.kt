package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ChecklistExportTest {
    private val today = LocalDate(2026, 10, 17)
    private val alice = Homie("u1", "Alice", Role.ADMIN)
    private val bob = Homie("u2", "Bob", Role.HOMIE)
    private val homies = listOf(alice, bob)

    private fun task(
        what: String,
        recurrence: Recurrence?,
        assignee: Assignee,
        minutes: Int = 90,
        pinned: Boolean = recurrence != null,
    ) = ChoreTask(
        id = what,
        what = what,
        where = "Kitchen, \"main\"",
        frequencyText = if (recurrence == null) "sometimes" else "1x Jahr",
        durationMinutes = minutes,
        assignee = assignee,
        recurrence = recurrence,
        nextDate = recurrence?.let { LocalDate(2027, 4, 4) },
        startTime = recurrence?.let { LocalTime(13, 45) },
        pinned = pinned,
    )

    private val tasks = listOf(
        task("yearly", Recurrence(RecurrenceUnit.YEAR, 1, DayOfWeek.SUNDAY, Ordinal.FIRST, month = 4), Assignee.of("u1")),
        task("monthly", Recurrence(RecurrenceUnit.MONTH, 2, DayOfWeek.SUNDAY, Ordinal.FIRST), Assignee.Everyone, 180),
        task("weekly", Recurrence(RecurrenceUnit.WEEK, 1, DayOfWeek.WEDNESDAY), Assignee.of("u1", "u2")),
        task("daily", Recurrence(RecurrenceUnit.DAY, 3), Assignee.Unassigned, 45),
        task("last", Recurrence(RecurrenceUnit.MONTH, 1, DayOfWeek.FRIDAY, Ordinal.LAST), Assignee.of("u2")),
        task("date", Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 15), Assignee.Everyone),
        task("unfilled", null, Assignee.Unassigned, 60),
    )

    private fun fingerprint(list: List<ChoreTask>) = list.map {
        listOf(it.what, it.where, it.durationMinutes, it.assignee, it.recurrence, it.nextDate, it.startTime, it.pinned)
    }

    @Test
    fun roundTripsPinnedRowsThroughExportAndImport() {
        val csv = ChecklistCsv.export(tasks, homies)

        val parsed = ChecklistCsv.parse(csv)
        val restored = TaskPlanner.plan(parsed.rows, homies, today)

        assertTrue(parsed.skippedLines.isEmpty())
        assertEquals(tasks.size, restored.size)
        assertEquals(fingerprint(tasks), fingerprint(restored))
        assertEquals("sometimes", restored.last().frequencyText)
        assertNull(restored.last().recurrence)
    }

    @Test
    fun writesHeaderAndCanonicalColumns() {
        val csv = ChecklistCsv.export(listOf(tasks[0]), homies).lines()

        assertEquals(ChecklistCsv.EXPORT_HEADER, csv[0])
        assertEquals(
            "yearly,\"Kitchen, \"\"main\"\"\",1x Jahr,90min,Alice,\"Every 1 year(s), 1st Sunday, month 4\"," +
                "2027-04-04 13:45,true",
            csv[1],
        )
    }

    @Test
    fun treatsMissingPinnedAsPinnedAndFalseAsComputed() {
        val rows = ChecklistCsv.parse(
            "What,Where,Frequency,Duration,Assignee,Repetition,Next Start,Pinned\n" +
                "a,w,1x Jahr,1h,Alice,Every 1 week(s) Sunday,2027-01-03 09:15,\n" +
                "b,w,1x Jahr,1h,Alice,Every 1 week(s), Sunday,2027-01-03 09:15,false\n" +
                "c,w,1x Jahr,1h,Alice,,not a date,\n",
        ).rows

        val planned = TaskPlanner.plan(rows, homies, today)

        assertTrue(planned[0].recurrence == null)
        assertTrue(planned[1].pinned.not() && planned[1].nextDate == LocalDate(2026, 10, 18))
        assertTrue(TaskWarning.INVALID_START in planned[2].warnings)
        assertTrue(planned[2].recurrence != null && !planned[2].pinned)
    }

    @Test
    fun keepsImportingFilesWithOnlyTheOriginalColumns() {
        val parsed = ChecklistCsv.parse(ChecklistCsv.TEMPLATE)

        val planned = TaskPlanner.plan(parsed.rows, homies, today)

        assertEquals(3, planned.size)
        assertTrue(planned.none { it.pinned })
        assertTrue(planned.all { it.recurrence != null })
    }

    @Test
    fun parsesEveryRepetitionFormAndRejectsUnknownText() {
        val sunday = DayOfWeek.SUNDAY
        val expected = mapOf(
            "Every 1 year(s), 1st Sunday, month 4" to Recurrence(RecurrenceUnit.YEAR, 1, sunday, Ordinal.FIRST, month = 4),
            "every 6 MONTH(S), 1st sunday" to Recurrence(RecurrenceUnit.MONTH, 6, sunday, Ordinal.FIRST),
            "Every 1 week(s),Sunday" to Recurrence(RecurrenceUnit.WEEK, 1, sunday),
            "Every 3 day(s)" to Recurrence(RecurrenceUnit.DAY, 3),
            "Every 1 year(s), day 15, month 8" to Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 15),
            "Every  2  months ,  last  Fri" to Recurrence(RecurrenceUnit.MONTH, 2, DayOfWeek.FRIDAY, Ordinal.LAST),
        )

        expected.forEach { (text, recurrence) -> assertEquals(recurrence, RecurrenceText.parse(text), text) }

        listOf("sometimes", "Every 0 day(s)", "Every 1 day(s), Sunday", "Every 1 week(s), month 3", "Every 1 year(s), day 40")
            .forEach { assertNull(RecurrenceText.parse(it), it) }
    }
}
