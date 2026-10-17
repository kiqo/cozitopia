package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

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

    private fun ids(scope: ExportScope) = IcsExporter.filter(tasks, "me", scope).map { it.id }

    @Test
    fun filtersByScope() {
        assertEquals(listOf("mine", "shared", "everyone", "nobody"), ids(ExportScope.Own))
        assertEquals(
            listOf("mine", "shared", "theirs", "everyone", "nobody"),
            ids(ExportScope.OwnAndSelected(setOf("other"))),
        )
        assertEquals(tasks.map { it.id }, ids(ExportScope.All))
    }

    @Test
    fun exportsCalendarWithCrlfAndRecurrence() {
        val ics = IcsExporter.export(listOf(task("t1", Assignee.Everyone, "Clean", "Kitchen")), "MyHome", generatedAt)

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
        val ics = IcsExporter.export(listOf(task("t", Assignee.Everyone, "a;b,c\\d\ne\r\nf")), "Home, Sweet", generatedAt)

        assertTrue("SUMMARY:a\\;b\\,c\\\\d\\ne\\nf" in ics.split("\r\n"))
        assertTrue("X-WR-CALNAME:Home\\, Sweet" in ics.split("\r\n"))
    }

    @Test
    fun foldsLongLinesWithoutSplittingMultibyteCharacters() {
        val what = "ä".repeat(60) + "😀".repeat(20) + "x".repeat(200)

        val ics = IcsExporter.export(listOf(task("t", Assignee.Everyone, what)), "Home", generatedAt)

        val physical = ics.split("\r\n").filter { it.isNotEmpty() }
        val unfolded = ics.replace("\r\n ", "")
        assertTrue(physical.all { it.encodeToByteArray().size <= 75 })
        assertTrue(physical.any { it.startsWith(" ") })
        assertTrue("SUMMARY:$what\r\n" in unfolded)
    }
}
