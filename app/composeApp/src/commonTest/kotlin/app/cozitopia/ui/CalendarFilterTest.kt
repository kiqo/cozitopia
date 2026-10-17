package app.cozitopia.ui

import app.cozitopia.domain.Assignee
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.ExportScope
import app.cozitopia.domain.IcsExporter
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.ui.calendar.CalendarFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class CalendarFilterTest {
    private fun task(id: String, assignee: Assignee) = ChoreTask(
        id, id, "", "", 60, assignee, Recurrence(RecurrenceUnit.YEAR), LocalDate(2026, 10, 17),
    )

    private val tasks = listOf(
        task("mine", Assignee.of("me")),
        task("theirs", Assignee.of("other")),
        task("everyone", Assignee.Everyone),
        task("nobody", Assignee.Unassigned),
    )

    @Test
    fun meFilterMatchesTheOwnExportScope() {
        val filtered = tasks.filter(CalendarFilter.Me("me")::matches).map { it.id }

        val exported = IcsExporter.filter(tasks, "me", ExportScope.Own).map { it.id }

        assertEquals(listOf("mine", "everyone", "nobody"), filtered)
        assertEquals(exported, filtered)
    }

    @Test
    fun personFilterMatchesOnlyThatPerson() {
        val filtered = tasks.filter(CalendarFilter.Person("other")::matches).map { it.id }

        assertEquals(listOf("theirs"), filtered)
    }
}
