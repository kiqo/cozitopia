package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class IcsExporterTest {
    private val generatedAt = Instant.parse("2026-10-17T09:05:07Z")
    private val recurrence = Recurrence(RecurrenceUnit.YEAR)

    private fun task(id: String, assignee: Assignee, what: String = id, where: String = "") =
        ChoreTask(id, what, where, "", 60, assignee, recurrence, LocalDate(2026, 10, 17))

    private val tasks = listOf(
        task("mine", Assignee.of("me")),
        task("shared", Assignee.of("me", "other")),
        task("theirs", Assignee.of("other")),
        task("third", Assignee.of("third")),
        task("everyone", Assignee.Everyone),
        task("nobody", Assignee.Unassigned),
    )

    private fun ids(scope: ExportScope) = IcsExporter.filter(tasks, scope).map { it.id }

    @Test
    fun filtersByScope() {
        assertEquals(listOf("mine", "shared", "everyone", "nobody"), ids(ExportScope.Single("me")))
        assertEquals(listOf("shared", "theirs", "everyone", "nobody"), ids(ExportScope.Single("other")))
        assertEquals(tasks.map { it.id }, ids(ExportScope.All))
    }

    @Test
    fun buildsSanitisedFileNames() {
        assertEquals("cozitopia_calendar_all.ics", IcsExporter.fileName(null))
        assertEquals("cozitopia_calendar_Jürgen_O_Neil.ics", IcsExporter.fileName(" Jürgen O'/Neil "))
        assertEquals("cozitopia_calendar_all.ics", IcsExporter.fileName("///"))
    }

    @Test
    fun exportsCalendarWithCrlfAndRecurrence() {
        val ics = IcsExporter.export(listOf(task("t1", Assignee.Everyone, "Clean", "Kitchen")), "MyHome", emptyList(), generatedAt)

        val lines = ics.split("\r\n")

        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
        assertFalse(ics.replace("\r\n", "").contains('\n'))
        assertTrue("UID:t1@cozitopia.app" in lines)
        assertTrue("DTSTAMP:20261017T090507Z" in lines)
        assertTrue("DTSTART;VALUE=DATE:20261017" in lines)
        assertTrue("DTEND;VALUE=DATE:20261018" in lines)
        assertTrue("RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=1;BYMONTHDAY=1" in lines)
        assertTrue("SUMMARY:Clean" in lines)
        assertTrue("LOCATION:Kitchen" in lines)
    }

    @Test
    fun escapesSpecialCharacters() {
        val ics = IcsExporter.export(listOf(task("t", Assignee.Everyone, "a;b,c\\d\ne\r\nf")), "Home, Sweet", emptyList(), generatedAt)

        assertTrue("SUMMARY:a\\;b\\,c\\\\d\\ne\\nf" in ics.split("\r\n"))
        assertTrue("X-WR-CALNAME:Home\\, Sweet" in ics.split("\r\n"))
    }

    @Test
    fun foldsLongLinesWithoutSplittingMultibyteCharacters() {
        val what = "ä".repeat(60) + "😀".repeat(20) + "x".repeat(200)

        val ics = IcsExporter.export(listOf(task("t", Assignee.Everyone, what)), "Home", emptyList(), generatedAt)

        val physical = ics.split("\r\n").filter { it.isNotEmpty() }
        val unfolded = ics.replace("\r\n ", "")
        assertTrue(physical.all { it.encodeToByteArray().size <= 75 })
        assertTrue(physical.any { it.startsWith(" ") })
        assertTrue("SUMMARY:$what\r\n" in unfolded)
    }

    @Test
    fun exportsTimedEventsWithFloatingTimeAndAllDayForZeroDuration() {
        val timed = task("timed", Assignee.Everyone).copy(startTime = LocalTime(23, 30), durationMinutes = 90)
        val untimed = task("zero", Assignee.Everyone).copy(startTime = LocalTime(10, 0), durationMinutes = 0)

        val ics = IcsExporter.export(listOf(timed, untimed), "MyHome", emptyList(), generatedAt)

        assertTrue(ics.contains("DTSTART:20261017T233000\r\n"))
        assertTrue(ics.contains("DTEND:20261018T010000\r\n"))
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261017\r\n"))
    }
}
